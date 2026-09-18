
import asyncio
import json

import pytest

from app.models.report import AiReportRequest
from app.services.report_service import (
    ReportGenerationError,
    ReportService,
)


def create_test_request():
    return AiReportRequest.model_validate({
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
    })


class FakeOpenRouterClient:

    def __init__(self, response: str):
        self.response = response

    async def complete(self, system_prompt, user_prompt):
        return self.response


def test_generate_weekly_report_success():

    ai_response = {
        "summary": "Haftalık genel değerlendirme.",
        "nutritionAnalysis": "Beslenme analizi.",
        "workoutAnalysis": "Egzersiz analizi.",
        "waterAnalysis": "Su tüketimi analizi.",
        "recommendations": [
            "Su tüketimini düzenli takip et.",
            "Egzersizlerini haftaya dengeli dağıt."
        ]
    }

    async def run():
        client = FakeOpenRouterClient(
            json.dumps(ai_response)
        )

        service = ReportService(client)

        report = await service.generate_weekly_report(
            create_test_request()
        )

        assert report.user_id == 42
        assert report.summary == ai_response["summary"]
        assert len(report.recommendations) == 2

        assert str(report.report_id) == (
            "550e8400-e29b-41d4-a716-446655440000"
        )

        result = report.model_dump(
            mode="json",
            by_alias=True
        )

        assert "nutritionAnalysis" in result

    asyncio.run(run())


def test_generate_weekly_report_invalid_json():

    async def run():
        client = FakeOpenRouterClient(
            "This is not valid JSON"
        )

        service = ReportService(client)

        with pytest.raises(ReportGenerationError):
            await service.generate_weekly_report(
                create_test_request()
            )

    asyncio.run(run())


def test_generate_weekly_report_missing_fields():

    async def run():
        client = FakeOpenRouterClient(
            json.dumps({
                "summary": "Incomplete report"
            })
        )

        service = ReportService(client)

        with pytest.raises(ReportGenerationError):
            await service.generate_weekly_report(
                create_test_request()
            )

    asyncio.run(run())