
import asyncio
import logging

import aio_pika
import httpx

from app.config.settings import get_settings
from app.messaging.topology import (
    EXCHANGE_NAME,
    REQUEST_QUEUE,
    RESULT_ROUTING_KEY,
    setup_rabbitmq,
)
from app.models.report import AiReportRequest
from app.services.openrouter_client import OpenRouterClient
from app.services.report_service import ReportService


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

logger = logging.getLogger(__name__)


async def process_message(message, report_service, exchange) -> None:
    """Process one request and acknowledge it only after publishing the result."""

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

    # Only acknowledge the request after the result has been published.
    await message.ack()

    logger.info(
        "Report published and request acknowledged: reportId=%s",
        request.report_id,
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
                    except Exception:
                        logger.exception(
                            "Report processing failed; stopping worker. "
                            "Message remains unacknowledged. messageId=%s",
                            message.message_id,
                        )
                        raise


if __name__ == "__main__":
    asyncio.run(run_worker())