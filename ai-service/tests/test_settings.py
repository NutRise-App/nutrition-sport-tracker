from app.config.settings import Settings


def test_openrouter_key_is_read_from_environment(monkeypatch) -> None:
    monkeypatch.setenv("OPENROUTER_API_KEY", "test-key-not-real")
    settings = Settings(_env_file=None)
    assert settings.openrouter_api_key is not None
    assert settings.openrouter_api_key.get_secret_value() == "test-key-not-real"
