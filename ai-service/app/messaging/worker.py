import asyncio
import logging

import aio_pika
import httpx
from pydantic import ValidationError

from app.config.settings import get_settings
from app.messaging.topology import (
    DEAD_LETTER_ROUTING_KEY,
    EXCHANGE_NAME,
    REQUEST_QUEUE,
    RESULT_ROUTING_KEY,
    RETRY_ROUTING_KEY,
    setup_rabbitmq,
)
from app.models.report import AiReportRequest
from app.services.openrouter_client import (
    OpenRouterClient,
    OpenRouterError,
    OpenRouterTransientError,
)
from app.services.report_service import (
    ReportGenerationError,
    ReportService,
)


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

logger = logging.getLogger(__name__)

MAX_RETRIES = 2
RETRY_COUNT_HEADER = "x-nutrise-retry-count"


async def process_message(message, report_service, exchange) -> None:
    """Publish the generated result before acknowledging the request."""

    request = AiReportRequest.model_validate_json(message.body)

    logger.info(
        "Generating report: reportId=%s",
        request.report_id,
    )

    result = await report_service.generate_weekly_report(request)

    confirmation = await exchange.publish(
        aio_pika.Message(
            body=result.model_dump_json(by_alias=True).encode("utf-8"),
            content_type="application/json",
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            correlation_id=str(request.report_id),
            message_id=str(request.report_id),
        ),
        routing_key=RESULT_ROUTING_KEY,
        mandatory=True,
    )

    if confirmation is False:
        raise RuntimeError("RabbitMQ did not confirm result publication")

    await message.ack()

    logger.info(
        "Report published and request acknowledged: reportId=%s",
        request.report_id,
    )


def get_retry_count(message) -> int:
    headers = message.headers or {}
    count = headers.get(RETRY_COUNT_HEADER, 0)

    # Reject malformed retry metadata rather than allowing infinite retries.
    if type(count) is not int or count < 0:
        return MAX_RETRIES

    return count


async def route_failed_message(message, exchange, error: Exception) -> None:
    """Publish to retry/DLQ before acknowledging the original message."""

    retry_count = get_retry_count(message)

    retryable = isinstance(
        error,
        (OpenRouterTransientError, ReportGenerationError),
    )

    should_retry = retryable and retry_count < MAX_RETRIES

    if should_retry:
        routing_key = RETRY_ROUTING_KEY
        next_retry_count = retry_count + 1
        destination = "retry"
    else:
        routing_key = DEAD_LETTER_ROUTING_KEY
        next_retry_count = retry_count
        destination = "dead-letter"

    confirmation = await exchange.publish(
        aio_pika.Message(
            body=message.body,
            content_type=message.content_type or "application/json",
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            correlation_id=message.correlation_id,
            message_id=message.message_id,
            headers={
                RETRY_COUNT_HEADER: next_retry_count,
                "x-nutrise-error-type": type(error).__name__,
            },
        ),
        routing_key=routing_key,
        mandatory=True,
    )

    if confirmation is False:
        raise RuntimeError(
            f"RabbitMQ did not confirm publication to {destination}"
        )

    await message.ack()

    logger.warning(
        "AI report message routed to %s; reportId=%s, "
        "retryCount=%s, errorType=%s",
        destination,
        message.message_id,
        next_retry_count,
        type(error).__name__,
    )


async def run_worker() -> None:
    settings = get_settings()

    if (
        settings.rabbitmq_password is None
        or not settings.rabbitmq_password.get_secret_value()
    ):
        raise ValueError("RabbitMQ password is missing")

    if (
        settings.openrouter_api_key is None
        or not settings.openrouter_api_key.get_secret_value()
    ):
        raise ValueError("OpenRouter API key is missing")

    await setup_rabbitmq()

    connection = await aio_pika.connect_robust(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        login=settings.rabbitmq_user,
        password=settings.rabbitmq_password.get_secret_value(),
        virtualhost="/",
        timeout=10,
    )

    async with connection:
        channel = await connection.channel(
            publisher_confirms=True,
            on_return_raises=True,
        )

        await channel.set_qos(prefetch_count=1)

        request_queue = await channel.declare_queue(
            REQUEST_QUEUE,
            durable=True,
        )

        exchange = await channel.declare_exchange(
            EXCHANGE_NAME,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )

        async with httpx.AsyncClient() as http_client:
            openrouter_client = OpenRouterClient(
                settings=settings,
                http_client=http_client,
            )
            report_service = ReportService(openrouter_client)

            logger.info("AI report worker is waiting for messages.")

            async with request_queue.iterator() as messages:
                async for message in messages:
                    try:
                        await process_message(
                            message,
                            report_service,
                            exchange,
                        )

                    except (
                        ValidationError,
                        OpenRouterError,
                        ReportGenerationError,
                    ) as error:
                        try:
                            await route_failed_message(
                                message,
                                exchange,
                                error,
                            )
                        except Exception:
                            logger.exception(
                                "Could not route failed message; "
                                "stopping without acknowledging. "
                                "messageId=%s",
                                message.message_id,
                            )
                            raise

                    except Exception:
                        logger.exception(
                            "Unexpected worker failure; stopping without "
                            "acknowledging. messageId=%s",
                            message.message_id,
                        )
                        raise


if __name__ == "__main__":
    asyncio.run(run_worker())