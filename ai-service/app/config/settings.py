
from functools import lru_cache

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "NutRise AI Service"

    openrouter_api_key: SecretStr | None = None

    openrouter_base_url: str = (
        "https://openrouter.ai/api/v1"
    )

    openrouter_model: str = "openai/gpt-4o-mini"

    openrouter_temperature: float = Field(
        default=0.6,
        ge=0,
        le=2
    )

    openrouter_timeout_seconds: float = Field(
        default=30.0,
        gt=0
    )

    openrouter_http_referer: str | None = None

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()