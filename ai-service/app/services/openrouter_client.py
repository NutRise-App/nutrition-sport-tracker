
import httpx

from app.config.settings import Settings


class OpenRouterError(Exception):
    """OpenRouter request or response error."""


class OpenRouterTransientError(OpenRouterError):
    """Temporary error that may be retried."""


class OpenRouterClient:

    def __init__(
        self,
        settings: Settings,
        http_client: httpx.AsyncClient | None = None
    ):
        self.settings = settings
        self.http_client = http_client

    async def complete(
        self,
        system_prompt: str,
        user_prompt: str
    ) -> str:

        if not system_prompt.strip():
            raise ValueError("System prompt cannot be empty")

        if not user_prompt.strip():
            raise ValueError("User prompt cannot be empty")

        api_key = self.settings.openrouter_api_key

        if api_key is None or not api_key.get_secret_value():
            raise OpenRouterError(
                "OpenRouter API key is missing"
            )

        headers = {
            "Authorization": (
                f"Bearer {api_key.get_secret_value()}"
            ),
            "Content-Type": "application/json"
        }

        if self.settings.openrouter_http_referer:
            headers["HTTP-Referer"] = (
                self.settings.openrouter_http_referer
            )

        payload = {
            "model": self.settings.openrouter_model,
            "temperature": (
                self.settings.openrouter_temperature
            ),
            "messages": [
                {
                    "role": "system",
                    "content": system_prompt
                },
                {
                    "role": "user",
                    "content": user_prompt
                }
            ]
        }

        url = (
            self.settings.openrouter_base_url.rstrip("/")
            + "/chat/completions"
        )

        if self.http_client is not None:
            return await self._send(
                self.http_client,
                url,
                headers,
                payload
            )

        async with httpx.AsyncClient() as client:
            return await self._send(
                client,
                url,
                headers,
                payload
            )

    async def _send(
        self,
        client: httpx.AsyncClient,
        url: str,
        headers: dict,
        payload: dict
    ) -> str:

        try:
            response = await client.post(
                url,
                headers=headers,
                json=payload,
                timeout=self.settings.openrouter_timeout_seconds
            )

            response.raise_for_status()

        except httpx.HTTPStatusError as exc:

            status = exc.response.status_code

            if status == 429 or status >= 500:
                raise OpenRouterTransientError(
                    f"OpenRouter temporary HTTP error: {status}"
                ) from exc

            raise OpenRouterError(
                f"OpenRouter HTTP error: {status}"
            ) from exc

        except httpx.TimeoutException as exc:
            raise OpenRouterTransientError(
                "OpenRouter request timed out"
            ) from exc

        except httpx.TransportError as exc:
            raise OpenRouterTransientError(
                "OpenRouter connection failed"
            ) from exc

        try:
            body = response.json()

            content = (
                body["choices"][0]["message"]["content"]
            )

            if not isinstance(content, str):
                raise ValueError("Invalid content type")

            if not content.strip():
                raise ValueError("Empty AI response")

            return content

        except (
            ValueError,
            KeyError,
            IndexError,
            TypeError
        ) as exc:

            raise OpenRouterError(
                "Invalid OpenRouter response format"
            ) from exc