from fastapi import FastAPI

from app.config.settings import get_settings

app = FastAPI(title="NutRise AI Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": get_settings().app_name}
