
from datetime import date
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator
from pydantic.alias_generators import to_camel


class ReportBaseModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid"
    )


class WeeklyStats(ReportBaseModel):
    average_daily_calories_in: float | None = Field(
        default=None, ge=0
    )
    average_daily_calories_burned: float | None = Field(
        default=None, ge=0
    )
    average_daily_protein_g: float | None = Field(
        default=None, ge=0
    )
    average_daily_carbs_g: float | None = Field(
        default=None, ge=0
    )
    average_daily_fat_g: float | None = Field(
        default=None, ge=0
    )
    average_daily_water_ml: float | None = Field(
        default=None, ge=0
    )
    total_workout_minutes: int | None = Field(
        default=None, ge=0
    )


class AiReportRequest(ReportBaseModel):
    report_id: UUID
    user_id: int = Field(gt=0)

    period_start: date
    period_end: date

    logged_days: int = Field(ge=1, le=7)

    goal: str | None = None

    weekly_stats: WeeklyStats

    @model_validator(mode="after")
    def validate_report_period(self):
        period_days = (
            self.period_end - self.period_start
        ).days + 1

        if period_days != 7:
            raise ValueError(
                "Weekly report period must contain exactly 7 days"
            )

        return self


class AiReportResponse(ReportBaseModel):
    report_id: UUID
    user_id: int = Field(gt=0)

    summary: str = Field(min_length=1)
    nutrition_analysis: str = Field(min_length=1)
    workout_analysis: str = Field(min_length=1)
    water_analysis: str = Field(min_length=1)

    recommendations: list[str] = Field(
        min_length=2,
        max_length=4
    )