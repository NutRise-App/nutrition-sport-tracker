# NutRise AI Service — Sprint Issue #1

Standalone FastAPI scaffold. Report generation, RabbitMQ consumers and producers, and OpenRouter calls are intentionally **not implemented yet**.

## Run locally (Python 3.12+)

```bash
cp .env.example .env
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements-dev.txt
uvicorn app.main:app --reload --port 8000
```

Check `http://127.0.0.1:8000/health` (HTTP 200, status ok), or open `http://127.0.0.1:8000/docs`.

## Tests

```bash
python -m pytest -q
```

## Docker

```bash
cp .env.example .env # skip if already created

docker build -t nutrise-ai-service .
docker run --rm -p 8000:8000 --env-file .env nutrise-ai-service
```

Then run `curl -i http://localhost:8000/health` from another terminal.

Do not commit `.env`, actual API keys, or personal user data. `.env.example` contains only empty/example configuration. The health endpoint checks the HTTP process, **not** RabbitMQ or OpenRouter availability. Python's future RabbitMQ worker should run as a separate process using the same app package, not as an additional HTTP request handler.
