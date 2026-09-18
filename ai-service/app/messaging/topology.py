
import asyncio
import aio_pika
from pydantic import SecretStr
from app.config.settings import get_settings


EXCHANGE_NAME = "nutrise.ai.exchange"

REQUEST_QUEUE = "nutrise.ai.report.request.queue"
RESULT_QUEUE = "nutrise.ai.report.result.queue"

REQUEST_ROUTING_KEY = "ai.report.requested"
RESULT_ROUTING_KEY = "ai.report.generated"


async def setup_rabbitmq():
    settings = get_settings()

    if not settings.rabbitmq_password:
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

        request_queue = await channel.declare_queue(
            REQUEST_QUEUE,
            durable=True,
        )

        result_queue = await channel.declare_queue(
            RESULT_QUEUE,
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

        print("RabbitMQ topology initialized successfully!")


if __name__ == "__main__":
    asyncio.run(setup_rabbitmq())