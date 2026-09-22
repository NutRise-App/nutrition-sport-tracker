import asyncio
import time
import uuid
from types import SimpleNamespace
from unittest.mock import AsyncMock

import aio_pika
from pydantic import ValidationError

from app.config.settings import get_settings
from app.messaging.topology import RETRY_DELAY_MS
from app.messaging.worker import (
    MAX_RETRIES,
    RETRY_COUNT_HEADER,
    process_message,
    route_failed_message,
)
from app.services.openrouter_client import OpenRouterTransientError


async def wait_for_message(queue, timeout_seconds=10):
    deadline = time.monotonic() + timeout_seconds

    while time.monotonic() < deadline:
        message = await queue.get(fail=False)

        if message is not None:
            return message

        await asyncio.sleep(0.25)

    raise AssertionError(
        f"Message did not arrive within {timeout_seconds} seconds"
    )


async def publish(exchange, body, routing_key, message_id, headers=None):
    confirmation = await exchange.publish(
        aio_pika.Message(
            body=body,
            content_type="application/json",
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            message_id=message_id,
            correlation_id=message_id,
            headers=headers or {},
        ),
        routing_key=routing_key,
        mandatory=True,
    )

    assert confirmation is not False, "Publication was not confirmed"


async def main():
    settings = get_settings()
    test_id = uuid.uuid4().hex
    exchange_name = f"nutrise.ai.integration.{test_id}"

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

        exchange = await channel.declare_exchange(
            exchange_name,
            aio_pika.ExchangeType.DIRECT,
            durable=False,
            auto_delete=True,
        )

        request_queue = await channel.declare_queue(
            f"{exchange_name}.request",
            durable=False,
            exclusive=True,
            auto_delete=True,
        )

        retry_queue = await channel.declare_queue(
            f"{exchange_name}.retry",
            durable=False,
            exclusive=True,
            auto_delete=True,
            arguments={
                "x-message-ttl": RETRY_DELAY_MS,
                "x-dead-letter-exchange": exchange_name,
                "x-dead-letter-routing-key": "ai.report.requested",
            },
        )

        dead_queue = await channel.declare_queue(
            f"{exchange_name}.dead",
            durable=False,
            exclusive=True,
            auto_delete=True,
        )

        await request_queue.bind(
            exchange, routing_key="ai.report.requested"
        )
        await retry_queue.bind(
            exchange, routing_key="ai.report.retry"
        )
        await dead_queue.bind(
            exchange, routing_key="ai.report.failed"
        )

        # TEST 1: Bozuk istek -> doğrudan DLQ.
        invalid_id = f"{test_id}-invalid"

        await publish(
            exchange,
            b"not-valid-json",
            "ai.report.requested",
            invalid_id,
        )

        incoming = await wait_for_message(request_queue)

        fake_service = SimpleNamespace(
            generate_weekly_report=AsyncMock()
        )

        try:
            await process_message(incoming, fake_service, exchange)
        except ValidationError as error:
            await route_failed_message(incoming, exchange, error)
        else:
            raise AssertionError("Invalid JSON was unexpectedly accepted")

        fake_service.generate_weekly_report.assert_not_awaited()

        dead_message = await wait_for_message(dead_queue)

        assert dead_message.message_id == invalid_id
        assert dead_message.body == b"not-valid-json"
        assert (
            dead_message.headers["x-nutrise-error-type"]
            == "ValidationError"
        )

        await dead_message.ack()
        print("PASS 1: Invalid JSON reached DLQ without AI call.", flush=True)

        # TEST 2: Simüle edilen geçici hata -> retry -> DLQ.
        retry_id = f"{test_id}-retry"

        await publish(
            exchange,
            b"isolated-retry-test",
            "ai.report.requested",
            retry_id,
            headers={RETRY_COUNT_HEADER: MAX_RETRIES - 1},
        )

        original = await wait_for_message(request_queue)
        started = time.monotonic()

        await route_failed_message(
            original,
            exchange,
            OpenRouterTransientError("Simulated timeout"),
        )

        await asyncio.sleep(2)

        early_message = await request_queue.get(fail=False)
        assert early_message is None, "Retry returned too early"

        returned = await wait_for_message(
            request_queue,
            timeout_seconds=RETRY_DELAY_MS / 1000 + 15,
        )

        elapsed = time.monotonic() - started

        assert returned.message_id == retry_id
        assert returned.headers[RETRY_COUNT_HEADER] == MAX_RETRIES
        assert elapsed >= RETRY_DELAY_MS / 1000 - 1

        print(
            f"PASS 2: Retry returned after {elapsed:.1f} seconds.",
            flush=True,
        )

        await route_failed_message(
            returned,
            exchange,
            OpenRouterTransientError("Simulated timeout"),
        )

        exhausted = await wait_for_message(dead_queue)

        assert exhausted.message_id == retry_id
        assert exhausted.headers[RETRY_COUNT_HEADER] == MAX_RETRIES
        assert (
            exhausted.headers["x-nutrise-error-type"]
            == "OpenRouterTransientError"
        )

        await exhausted.ack()
        print("PASS 3: Exhausted retry reached DLQ.", flush=True)


if __name__ == "__main__":
    asyncio.run(main())