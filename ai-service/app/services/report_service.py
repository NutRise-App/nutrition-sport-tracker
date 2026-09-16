
import json

from pydantic import ValidationError

from app.models.report import (
    AiReportRequest,
    AiReportResponse,
)
from app.prompts.report_prompt import (
    SYSTEM_PROMPT,
    build_weekly_report_prompt,
)
from app.services.openrouter_client import OpenRouterClient


class ReportGenerationError(Exception):
    """AI report generation returned an invalid result."""


class ReportService:

    def __init__(self, openrouter_client: OpenRouterClient):
        self.openrouter_client = openrouter_client

    async def generate_weekly_report(
        self,
        request: AiReportRequest
    ) -> AiReportResponse:

        # 1. Kullanıcı verilerinden prompt oluştur.
        user_prompt = build_weekly_report_prompt(request)

        # 2. OpenRouter üzerinden AI raporu oluştur.
        ai_text = await self.openrouter_client.complete(
            system_prompt=SYSTEM_PROMPT,
            user_prompt=user_prompt
        )

        # 3. AI yanıtını JSON'a dönüştür.
        try:
            report_data = json.loads(ai_text)

            if not isinstance(report_data, dict):
                raise ValueError(
                    "AI response must be a JSON object"
                )

        except (json.JSONDecodeError, ValueError) as exc:
            raise ReportGenerationError(
                "AI returned invalid JSON"
            ) from exc

        # 4. AI çıktısını doğrula ve rapor kimliklerini ekle.
        # Kimlikleri AI'dan değil, orijinal request'ten alıyoruz.
        report_data["reportId"] = str(request.report_id)
        report_data["userId"] = request.user_id

        try:
            return AiReportResponse.model_validate(
                report_data
            )

        except ValidationError as exc:
            raise ReportGenerationError(
                "AI report does not match the response schema"
            ) from exc