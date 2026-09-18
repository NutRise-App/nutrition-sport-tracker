
import asyncio

import httpx
import pytest

from app.config.settings import Settings
from app.services.openrouter_client import (
    OpenRouterClient,
    OpenRouterTransientError,
)


def test_openrouter_success():
    """Başarılı API yanıtının doğru işlendiğini test eder."""

    def handler(request: httpx.Request):
        assert request.method == "POST"
        assert request.url.path == "/api/v1/chat/completions"
        assert request.headers["Authorization"] == "Bearer test-key"

        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": '{"summary":"Test report"}'
                        }
                    }
                ]
            }
        )

    async def run():
        settings = Settings(
            openrouter_api_key="test-key",
            _env_file=None
        )

        transport = httpx.MockTransport(handler)

        async with httpx.AsyncClient(
            transport=transport
        ) as http_client:
            client = OpenRouterClient(settings, http_client)

            result = await client.complete(
                "You are a report assistant.",
                "Generate a test report."
            )

            assert result == '{"summary":"Test report"}'

    asyncio.run(run())


def test_openrouter_server_error():
    """HTTP 503 hatasının geçici hata olarak sınıflandırıldığını test eder."""

    def handler(request: httpx.Request):
        return httpx.Response(
            503,
            json={"error": "Service unavailable"}
        )

    async def run():
        settings = Settings(
            openrouter_api_key="test-key",
            _env_file=None
        )

        transport = httpx.MockTransport(handler)

        async with httpx.AsyncClient(
            transport=transport
        ) as http_client:
            client = OpenRouterClient(settings, http_client)

            with pytest.raises(OpenRouterTransientError):
                await client.complete(
                    "System prompt",
                    "User prompt"
                )

    asyncio.run(run())