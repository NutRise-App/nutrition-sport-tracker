import asyncio
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock

import aio_pika
import pytest

from app.messaging.topology import (
    DEAD_LETTER_ROUTING_KEY,
    RESULT_ROUTING_KEY,
    RETRY_ROUTING_KEY,
)
from app.messaging.worker import (
    MAX_RETRIES,
    RETRY_COUNT_HEADER,
    get_retry_count,
    process_message,
    route_failed_message,
)
from app.models.report import AiReportResponse
from app.services.openrouter_client import (
    OpenRouterError,
    OpenRouterTransientError,
)
from app.services.report_service import ReportGenerationError


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


def make_failed_message(retry_count=0, body=None):
    if body is None:
        body = json.dumps(REQUEST).encode("utf-8")

    return SimpleNamespace(
        body=body,
        headers={RETRY_COUNT_HEADER: retry_count},
        content_type="application/json",
        correlation_id=REPORT_ID,
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


# ------------------------------------------------------------
# Başarılı rapor üretimi ve mevcut hata davranışı
# ------------------------------------------------------------

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


# ------------------------------------------------------------
# Retry davranışı
# ------------------------------------------------------------

def test_transient_error_routes_to_retry_before_ack():
    message = make_failed_message()
    _, exchange = make_dependencies()

    async def publish_then_confirm(*args, **kwargs):
        # Retry mesajı yayımlanmadan orijinal mesaj onaylanmamalı.
        message.ack.assert_not_awaited()
        return True

    exchange.publish.side_effect = publish_then_confirm

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            OpenRouterTransientError("Timeout"),
        )
    )

    published = exchange.publish.await_args.args[0]
    kwargs = exchange.publish.await_args.kwargs

    assert kwargs["routing_key"] == RETRY_ROUTING_KEY
    assert kwargs["mandatory"] is True
    assert published.body == message.body
    assert published.headers[RETRY_COUNT_HEADER] == 1
    assert published.delivery_mode == aio_pika.DeliveryMode.PERSISTENT

    message.ack.assert_awaited_once()


def test_second_retry_is_still_allowed():
    message = make_failed_message(retry_count=1)
    _, exchange = make_dependencies()

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            OpenRouterTransientError("HTTP 503"),
        )
    )

    published = exchange.publish.await_args.args[0]

    assert (
        exchange.publish.await_args.kwargs["routing_key"]
        == RETRY_ROUTING_KEY
    )
    assert published.headers[RETRY_COUNT_HEADER] == MAX_RETRIES

    message.ack.assert_awaited_once()


def test_exhausted_retries_route_to_dlq():
    message = make_failed_message(retry_count=MAX_RETRIES)
    _, exchange = make_dependencies()

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            OpenRouterTransientError("HTTP 503"),
        )
    )

    published = exchange.publish.await_args.args[0]

    assert (
        exchange.publish.await_args.kwargs["routing_key"]
        == DEAD_LETTER_ROUTING_KEY
    )
    assert published.headers[RETRY_COUNT_HEADER] == MAX_RETRIES

    message.ack.assert_awaited_once()


def test_invalid_ai_response_can_be_retried():
    message = make_failed_message()
    _, exchange = make_dependencies()

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            ReportGenerationError("Invalid AI JSON"),
        )
    )

    assert (
        exchange.publish.await_args.kwargs["routing_key"]
        == RETRY_ROUTING_KEY
    )
    message.ack.assert_awaited_once()


# ------------------------------------------------------------
# Kalıcı hata ve Dead Letter Queue
# ------------------------------------------------------------

def test_permanent_openrouter_error_goes_directly_to_dlq():
    message = make_failed_message()
    _, exchange = make_dependencies()

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            OpenRouterError("HTTP 401"),
        )
    )

    assert (
        exchange.publish.await_args.kwargs["routing_key"]
        == DEAD_LETTER_ROUTING_KEY
    )
    message.ack.assert_awaited_once()


def test_invalid_json_goes_to_dlq_without_calling_ai():
    message = make_failed_message(body=b"not-valid-json")
    report_service, exchange = make_dependencies()

    with pytest.raises(ValueError) as exc_info:
        asyncio.run(process_message(message, report_service, exchange))

    report_service.generate_weekly_report.assert_not_awaited()
    message.ack.assert_not_awaited()

    asyncio.run(
        route_failed_message(
            message,
            exchange,
            exc_info.value,
        )
    )

    assert (
        exchange.publish.await_args.kwargs["routing_key"]
        == DEAD_LETTER_ROUTING_KEY
    )
    message.ack.assert_awaited_once()


# ------------------------------------------------------------
# RabbitMQ yayın hataları: Orijinal mesaj korunmalı
# ------------------------------------------------------------

def test_failed_retry_publication_does_not_ack_original():
    message = make_failed_message()
    _, exchange = make_dependencies()

    exchange.publish.side_effect = RuntimeError(
        "RabbitMQ unavailable"
    )

    with pytest.raises(RuntimeError, match="RabbitMQ unavailable"):
        asyncio.run(
            route_failed_message(
                message,
                exchange,
                OpenRouterTransientError("Timeout"),
            )
        )

    message.ack.assert_not_awaited()


def test_unconfirmed_dlq_publication_does_not_ack_original():
    message = make_failed_message()
    _, exchange = make_dependencies()

    exchange.publish.return_value = False

    with pytest.raises(RuntimeError, match="did not confirm"):
        asyncio.run(
            route_failed_message(
                message,
                exchange,
                OpenRouterError("HTTP 401"),
            )
        )

    message.ack.assert_not_awaited()


def test_invalid_retry_header_cannot_cause_unlimited_retries():
    message = make_failed_message(retry_count="invalid")

    assert get_retry_count(message) == MAX_RETRIES