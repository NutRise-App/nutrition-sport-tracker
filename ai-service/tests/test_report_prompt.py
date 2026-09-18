
from app.models.report import AiReportRequest

from app.prompts.report_prompt import (
    SYSTEM_PROMPT,
    build_weekly_report_prompt,
)


def test_weekly_report_prompt():

    request = AiReportRequest.model_validate({
        "reportId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 42,
        "periodStart": "2026-09-07",
        "periodEnd": "2026-09-13",
        "loggedDays": 5,
        "goal": "MAINTAIN_WEIGHT",
        "weeklyStats": {
            "averageDailyCaloriesIn": 2100,
            "averageDailyWaterMl": 1800
        }
    })

    prompt = build_weekly_report_prompt(request)

    assert '"loggedDays": 5' in prompt
    assert '"averageDailyCaloriesIn": 2100.0' in prompt
    assert '"averageDailyWaterMl": 1800.0' in prompt

    # Eksik veriler sıfır olarak gönderilmemeli.
    assert '"averageDailyProteinG": null' in prompt

    # AI'a kullanıcı kimliği gönderilmiyor.
    assert '"userId"' not in prompt
    assert '"reportId"' not in prompt

    assert "Yalnızca geçerli JSON" in SYSTEM_PROMPT