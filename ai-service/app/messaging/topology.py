import asyncio

import aio_pika

from app.config.settings import get_settings


EXCHANGE_NAME = "nutrise.ai.exchange"

REQUEST_QUEUE = "nutrise.ai.report.request.queue"
RESULT_QUEUE = "nutrise.ai.report.result.queue"

RETRY_QUEUE = "nutrise.ai.report.retry.queue"
DEAD_LETTER_QUEUE = "nutrise.ai.report.dead-letter.queue"

REQUEST_ROUTING_KEY = "ai.report.requested"
RESULT_ROUTING_KEY = "ai.report.generated"
RETRY_ROUTING_KEY = "ai.report.retry"
DEAD_LETTER_ROUTING_KEY = "ai.report.failed"

RETRY_DELAY_MS = 30_000


async def setup_rabbitmq():
    settings = get_settings()

    if (
        settings.rabbitmq_password is None
        or not settings.rabbitmq_password.get_secret_value()
    ):
        raise ValueError("RabbitMQ password is missing")

    connection = await aio_pika.connect_robust(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        login=settings.rabbitmq_user,
        password=settings.rabbitmq_password.get_secret_value(),
        virtualhost="/",
        timeout=10,
    )

    async with connection:
        channel = await connection.channel()

        exchange = await channel.declare_exchange(
            EXCHANGE_NAME,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )

        # Mevcut kuyrukların özelliklerini değiştirmiyoruz.
        request_queue = await channel.declare_queue(
            REQUEST_QUEUE,
            durable=True,
        )

        result_queue = await channel.declare_queue(
            RESULT_QUEUE,
            durable=True,
        )

        # Mesaj burada 30 saniye bekler. Süresi dolunca RabbitMQ,
        # mesajı orijinal request routing key'iyle geri yönlendirir.
        retry_queue = await channel.declare_queue(
            RETRY_QUEUE,
            durable=True,
            arguments={
                "x-message-ttl": RETRY_DELAY_MS,
                "x-dead-letter-exchange": EXCHANGE_NAME,
                "x-dead-letter-routing-key": REQUEST_ROUTING_KEY,
            },
        )

        # İşlenemeyen mesajları incelemek için ayrı, kalıcı kuyruk.
        dead_letter_queue = await channel.declare_queue(
            DEAD_LETTER_QUEUE,
            durable=True,
        )

        await request_queue.bind(
            exchange,
            routing_key=REQUEST_ROUTING_KEY,
        )

        await result_queue.bind(
            exchange,
            routing_key=RESULT_ROUTING_KEY,
        )

        await retry_queue.bind(
            exchange,
            routing_key=RETRY_ROUTING_KEY,
        )

        await dead_letter_queue.bind(
            exchange,
            routing_key=DEAD_LETTER_ROUTING_KEY,
        )

        print("RabbitMQ topology initialized successfully!")


if __name__ == "__main__":
    asyncio.run(setup_rabbitmq())