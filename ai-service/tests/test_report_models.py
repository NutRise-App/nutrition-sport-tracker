
import pytest

from pydantic import ValidationError

from app.models.report import AiReportRequest


def test_valid_report_request():

    data = {
        "reportId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 42,
        "periodStart": "2026-09-07",
        "periodEnd": "2026-09-13",
        "loggedDays": 5,
        "goal": "MAINTAIN_WEIGHT",
        "weeklyStats": {
            "averageDailyCaloriesIn": 2100,
            "averageDailyCaloriesBurned": 350,
            "averageDailyProteinG": 110,
            "averageDailyCarbsG": 230,
            "averageDailyFatG": 65,
            "averageDailyWaterMl": 1800,
            "totalWorkoutMinutes": 180
        }
    }

    report = AiReportRequest.model_validate(data)

    assert report.user_id == 42
    assert report.logged_days == 5
    assert report.weekly_stats.average_daily_calories_in == 2100

    result = report.model_dump(
        mode="json",
        by_alias=True
    )

    assert result["userId"] == 42
    assert result["weeklyStats"]["totalWorkoutMinutes"] == 180


def test_invalid_report_period():

    data = {
        "reportId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 42,
        "periodStart": "2026-09-07",
        "periodEnd": "2026-09-12",
        "loggedDays": 5,
        "weeklyStats": {}
    }

    with pytest.raises(ValidationError):
        AiReportRequest.model_validate(data)


def test_missing_metrics_are_not_zero():

    data = {
        "reportId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 42,
        "periodStart": "2026-09-07",
        "periodEnd": "2026-09-13",
        "loggedDays": 1,
        "weeklyStats": {}
    }

    report = AiReportRequest.model_validate(data)

    assert report.weekly_stats.average_daily_water_ml is None