
import asyncio
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock

import aio_pika
import pytest

from app.messaging.topology import RESULT_ROUTING_KEY
from app.messaging.worker import process_message
from app.models.report import AiReportResponse


REPORT_ID = "550e8400-e29b-41d4-a716-446655440000"

REQUEST = {
    "reportId": REPORT_ID,
    "userId": 42,
    "periodStart": "2026-09-07",
    "periodEnd": "2026-09-13",
    "loggedDays": 5,
    "weeklyStats": {
        "averageDailyCaloriesIn": 1850,
        "averageDailyCaloriesBurned": 350,
        "averageDailyWaterMl": 1750,
        "totalWorkoutMinutes": 180,
    },
}

RESPONSE = AiReportResponse.model_validate(
    {
        "reportId": REPORT_ID,
        "userId": 42,
        "summary": "Örnek haftalık özet.",
        "nutritionAnalysis": "Örnek beslenme analizi.",
        "workoutAnalysis": "Örnek egzersiz analizi.",
        "waterAnalysis": "Örnek su tüketimi analizi.",
        "recommendations": [
            "Öneri bir.",
            "Öneri iki.",
        ],
    }
)


def make_message(body=None):
    if body is None:
        body = json.dumps(REQUEST).encode("utf-8")

    return SimpleNamespace(
        body=body,
        message_id=REPORT_ID,
        ack=AsyncMock(),
    )


def make_dependencies():
    report_service = SimpleNamespace(
        generate_weekly_report=AsyncMock(return_value=RESPONSE),
    )
    exchange = SimpleNamespace(
        publish=AsyncMock(return_value=True),
    )
    return report_service, exchange


def test_success_publishes_result_then_acknowledges_request():
    message = make_message()
    report_service, exchange = make_dependencies()

    asyncio.run(process_message(message, report_service, exchange))

    report_service.generate_weekly_report.assert_awaited_once()
    exchange.publish.assert_awaited_once()
    message.ack.assert_awaited_once()

    published_message = exchange.publish.await_args.args[0]
    publish_kwargs = exchange.publish.await_args.kwargs

    assert published_message.content_type == "application/json"
    assert published_message.delivery_mode == aio_pika.DeliveryMode.PERSISTENT
    assert published_message.correlation_id == REPORT_ID
    assert publish_kwargs["routing_key"] == RESULT_ROUTING_KEY
    assert publish_kwargs["mandatory"] is True

    published_body = json.loads(published_message.body)
    assert published_body["reportId"] == REPORT_ID
    assert published_body["userId"] == 42
    assert len(published_body["recommendations"]) == 2


def test_generation_failure_does_not_acknowledge_request():
    message = make_message()
    report_service, exchange = make_dependencies()

    report_service.generate_weekly_report.side_effect = RuntimeError(
        "AI service failed"
    )

    with pytest.raises(RuntimeError, match="AI service failed"):
        asyncio.run(process_message(message, report_service, exchange))

    exchange.publish.assert_not_awaited()
    message.ack.assert_not_awaited()


def test_publication_failure_does_not_acknowledge_request():
    message = make_message()
    report_service, exchange = make_dependencies()

    exchange.publish.side_effect = RuntimeError(
        "RabbitMQ publication failed"
    )

    with pytest.raises(RuntimeError, match="RabbitMQ publication failed"):
        asyncio.run(process_message(message, report_service, exchange))

    message.ack.assert_not_awaited()


def test_invalid_request_does_not_call_ai_or_acknowledge():
    message = make_message(body=b"not-valid-json")
    report_service, exchange = make_dependencies()

    with pytest.raises(ValueError):
        asyncio.run(process_message(message, report_service, exchange))

    report_service.generate_weekly_report.assert_not_awaited()
    exchange.publish.assert_not_awaited()
    message.ack.assert_not_awaited()