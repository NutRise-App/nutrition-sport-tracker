# NutRise Backend

NutRise Backend is the Spring Boot REST API powering **NutRise**, a nutrition and fitness tracking mobile application. This repository also contains a separate Python AI report worker connected to the Java backend through RabbitMQ.

The system provides secure authentication, nutrition and workout tracking, water intake management, daily summaries, scheduled reports, and asynchronous AI-generated weekly reports.

Beyond application development, the backend has been extended with production-oriented engineering practices including automated testing, Docker, CI/CD, database migrations, security scanning, health checks, and Railway deployment.

---

## 🚀 Production

The **Java backend** is deployed on **Railway** using the production Spring profile. The production URL and health check below describe the Java API; the local Docker Compose setup for RabbitMQ and the Python worker is documented separately and does not, by itself, establish their production deployment.

**Production API**

```text
https://nutrition-sport-tracker-production.up.railway.app
```

**Health Check**

```text
GET /actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

The production environment has been manually smoke-tested through the complete authentication flow:

```text
Health Check
     ↓
User Registration
     ↓
MySQL Persistence
     ↓
User Login
     ↓
BCrypt Password Verification
     ↓
JWT Generation
     ↓
JWT Authentication Filter
     ↓
Protected Endpoint
```

---

## ✨ Features

- User registration and login
- JWT-based stateless authentication
- BCrypt password hashing
- User profile management
- Meal logging and nutrition tracking
- Water intake tracking
- Workout logging
- Daily user summaries
- Daily and weekly scheduled reports
- Asynchronous AI-generated weekly reports using a Python worker and RabbitMQ
- Retry and dead-letter handling for unsuccessful AI report requests
- Report persistence and email delivery handled by the Java backend
- External API integrations
- MySQL persistence
- Versioned database migrations with Flyway
- Spring Boot Actuator health checks

---

## 🛠 Tech Stack

### Backend

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- JWT
- Maven

### Database

- MySQL
- Flyway

### AI Report Worker & Messaging

- Python (3.12 in the worker Docker image)
- FastAPI-based AI service
- aio-pika
- RabbitMQ (direct exchange and durable report queues)

### External Services

- OpenRouter
- USDA FoodData Central
- Gmail SMTP

### Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers
- pytest (Python unit tests)
- Manual RabbitMQ integration checks

### DevOps & Deployment

- Docker
- Docker Compose
- Multi-stage Docker builds
- GitHub Actions
- GitHub Container Registry (GHCR)
- Railway
- Dependabot
- CodeQL
- Trivy

---

## 🏗 Architecture

The backend follows a layered architecture:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
MySQL
```

Protected requests also pass through Spring Security and the JWT authentication filter.

```text
Request
  ↓
Spring Security
  ↓
JwtAuthenticationFilter
  ↓
Controller
  ↓
Service
  ↓
Repository
```

This structure separates API handling, business logic, persistence, and security responsibilities.

### Asynchronous AI Weekly Reports

The Java backend owns the public API, MySQL persistence, and email delivery. The Python worker generates the report text; it does **not** write directly to the Java database or send email.

```text
Authenticated client / scheduled Java flow
               ↓
       Spring Boot API
       POST /api/report/weekly/request
               ↓ 202 Accepted (QUEUED + reportId)
       RabbitMQ request queue
               ↓
       Python AI report worker
               ↓ OpenRouter
       RabbitMQ result queue
               ↓
       Java result consumer
               ↓
       MySQL report + email log
               ↓
       Java email delivery (SMTP)
```

Messages carry a `reportId`. The Java result-handling path checks for an existing report before saving it, helping prevent duplicate report records when RabbitMQ redelivers a message. An HTTP `202 Accepted` response means that processing was queued, **not** that the AI report or email has finished.

### Retries and Dead-Letter Queue

The worker publishes a successful result before acknowledging the original request. For failures:

- Temporary OpenRouter failures (timeouts, connection errors, HTTP `429` or `5xx`) and invalid AI-generated report content can be retried **up to two additional times**.
- Retry messages wait **30 seconds** in a dedicated TTL queue before RabbitMQ returns them to the request queue.
- Invalid incoming request data and permanent OpenRouter errors go directly to the dead-letter queue (DLQ).
- Messages that still fail after the retry limit also go to the DLQ.
- The original request is acknowledged only after the retry/DLQ publication is confirmed. A RabbitMQ publication failure leaves it unacknowledged instead of silently discarding it.

Existing request and result queues are unchanged; the worker declares separate retry and dead-letter queues.

| Queue | Role |
| --- | --- |
| `nutrise.ai.report.request.queue` | Incoming report requests |
| `nutrise.ai.report.retry.queue` | 30-second delay before another attempt |
| `nutrise.ai.report.result.queue` | Successfully generated reports for Java |
| `nutrise.ai.report.dead-letter.queue` | Invalid or exhausted requests for inspection |

RabbitMQ delivery is at-least-once: retrying or reconnecting can produce duplicate deliveries, so consumers should retain idempotency safeguards.

---

## 🔐 Authentication

NutRise uses JWT-based stateless authentication.

### Register

```text
POST /api/auth/register
```

### Login

```text
POST /api/auth/login
```

A successful login returns a JWT.

Protected endpoints require the token in the request header:

```http
Authorization: Bearer <token>
```

Example protected endpoint:

```text
GET /api/users/me
```

Requests without valid authentication receive:

```text
401 Unauthorized
```

Passwords are hashed with BCrypt and password fields are excluded from API responses.

---

## ⚙️ Environment Profiles

Application configuration is separated by environment.

```text
application.yml
application-dev.yml
application-prod.yml
application-test.yml
```

The Python AI worker has its own settings, independent of Spring profiles.

The active Spring profile can be selected using:

```bash
SPRING_PROFILES_ACTIVE=dev
```

Production credentials and sensitive configuration are not hard-coded in source files.

They are provided through environment variables.

---

## 🔑 Environment Variables

Important environment variables include:

```text
SPRING_PROFILES_ACTIVE

SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

JWT_SECRET
JWT_EXPIRATION

MAIL_USERNAME
MAIL_PASSWORD

OPENROUTER_API_KEY
OPENROUTER_HTTP_REFERER
OPENROUTER_X_TITLE

USDA_API_KEY

CORS_ALLOWED_ORIGIN_PATTERNS

SCHEDULING_ENABLED

# RabbitMQ / AI report worker (as applicable to each service)
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USER
RABBITMQ_PASSWORD
OPENROUTER_MODEL
```

The Java report-result listener can be enabled or disabled through `app.ai-report.result-consumer.enabled` (disabled by default in the listener configuration). Check `docker-compose.yml` and the relevant settings files for the exact environment-variable mapping.

Real credentials, passwords, JWT secrets, and API keys must never be committed to the repository.

Use `.env.example` as a reference when configuring the project locally.

---

## 🗄 Database Migrations

Database schema changes are managed with **Flyway**.

Migration files are stored under:

```text
src/main/resources/db/migration/
```

The project includes versioned migrations for the initial database schema, query indexes, AI report identifiers, email-log report references, and email delivery status.

Production uses Hibernate schema validation instead of automatically modifying the database:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

This allows Flyway to remain responsible for controlled and versioned schema changes.

---

## 🐳 Docker

The backend is containerized using a **multi-stage Docker build**.

The build stage compiles the Spring Boot application using Maven and Java 17.

The runtime stage contains only the required Java runtime and packaged application.

The production container also runs using a **non-root user**.

Build the backend image locally:

```bash
docker build -t nutrise-backend:local .
```

---

## 🐳 Docker Compose

Docker Compose runs the **local** Java backend, MySQL, RabbitMQ, and Python AI worker. The Java backend uses
MySQL and exchanges asynchronous report messages with the worker through RabbitMQ.

```text
React Native client
       ↓
Spring Boot backend ───→ MySQL
       │                    ↑
       ↓ report request     │ report + email log
     RabbitMQ               │
       ↓                    │
  Python AI worker          │
       ↓ OpenRouter         │
RabbitMQ result ─────────→ Java result consumer
                                ↓
                         Java SMTP delivery
```

From the **repository root**, configure the required environment variables using `.env.example` as a
reference and start the local stack:

```bash
docker compose up -d --build
docker compose ps
```

The worker initializes the retry and DLQ queues on startup. To inspect queue depth while RabbitMQ is running:

```bash
docker compose exec -T rabbitmq \
  rabbitmqctl list_queues -p / name messages_ready messages_unacknowledged
```

Stop the stack without deleting database volumes:

```bash
docker compose down
```

**Data-loss warning:** `docker compose down -v` also removes Compose-managed volumes, including local
database data. Use it only when you explicitly intend to erase that data.

---

## 🩺 Health Check & Graceful Shutdown

Spring Boot Actuator is used to expose the application health endpoint:

```text
/actuator/health
```

Only the required health information is publicly exposed.

The application also supports graceful shutdown so active work can terminate cleanly when the container receives a shutdown signal.

Example production health response:

```json
{
  "status": "UP"
}
```

---

## 🧪 Testing

### Java backend

From the repository root, run Maven tests:

```bash
./mvnw clean test
./mvnw clean verify
```

The Java suite covers authentication, JWT/security behavior, domain services, repositories, and persistence.
Database integration tests use **Testcontainers** with MySQL rather than a developer's local database.

### Python AI worker

With the AI service's Python dependencies and `pytest` installed in your virtual environment:

```bash
cd ai-service
python -m pytest -q
```

The Python tests cover report models and generation, OpenRouter error handling, result publication,
retry limits, dead-letter routing, and the rule that a request must not be acknowledged before
its output or failure message is confirmed. **24 Python tests passed in the Issue #7 verification.**

### RabbitMQ integration check (manual)

The integration script creates **isolated, temporary** RabbitMQ queues. It does not send a real AI request,
create a Java report, or send email. Start RabbitMQ first, then run this command from the repository root:

```bash
docker compose exec -T ai-worker python - < ai-service/tests/rabbitmq_integration_check.py
```

The script checks all three scenarios:

1. Invalid request JSON reaches the DLQ without calling AI.
2. A simulated temporary failure returns from the retry queue after approximately 30 seconds.
3. Exhausted retries reach the DLQ.

The three checks passed during Issue #7 verification. **This integration check is currently manual and
is not wired into GitHub Actions.** It tests the Python routing functions against a real RabbitMQ
broker using a simulated error; it does not test a live OpenRouter failure or a real email delivery.

---

## 🔄 CI/CD

The project uses **GitHub Actions** for automated build, test, container publishing, and security checks.

The existing CI/CD pipeline provides automated backend verification before production changes are deployed. The RabbitMQ integration check described above is manual, not a CI job; do not interpret its local result as a GitHub Actions check.

### Backend CI

Backend changes are automatically validated using Maven.

The pipeline runs the project's tests and build verification before changes are merged.

### Container Publishing

Docker images are built automatically and published to **GitHub Container Registry (GHCR)**.

Versioned image tags make container builds traceable to their corresponding source code revision.

### Railway Deployment

The production backend is connected to the GitHub repository through Railway.

Production deployments use the `master` branch and are automatically triggered after accepted changes.

Railway verifies the application through:

```text
/actuator/health
```

A deployment is considered healthy only when the application successfully starts and responds to the health check.

---

## 🛡 Security

NutRise includes multiple application and DevSecOps security measures.

### Application Security

- JWT-based authentication
- BCrypt password hashing
- Stateless Spring Security configuration
- Protected API endpoints
- Controlled CORS configuration
- Externalized production secrets
- Production environment validation
- Non-root Docker runtime
- Password fields excluded from API responses

### Dependabot

Dependabot is enabled to identify and propose dependency updates.

### CodeQL

GitHub CodeQL performs automated static analysis of the source code.

### Trivy

Trivy scans container images for known vulnerabilities.

### Responsible Disclosure

Security-related information and reporting instructions are documented in:

```text
SECURITY.md
```

---

## 📁 Project Structure

```text
nutrition-sport-tracker/
├── .github/
│   ├── dependabot.yml
│   └── workflows/
├── ai-service/
│   ├── app/
│   │   ├── config/
│   │   ├── messaging/        # RabbitMQ topology and worker
│   │   ├── models/
│   │   ├── prompts/
│   │   └── services/         # OpenRouter client and report generation
│   ├── tests/
│   │   ├── test_worker.py
│   │   └── rabbitmq_integration_check.py
│   ├── Dockerfile
│   └── requirements.txt
├── src/
│   ├── main/
│   │   ├── java/com/example/nutritionsporttracker/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
├── .dockerignore
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── SECURITY.md
├── pom.xml
└── README.md
```

---

## 📱 About NutRise

NutRise is a full-stack nutrition and fitness tracking application.

The system consists of:

```text
React Native mobile application
             ↓
       Spring Boot REST API ─────→ MySQL
             │
             ↓
          RabbitMQ ↔ Python AI report worker ↔ OpenRouter
             │
             ↓
       Java report consumer → MySQL + SMTP email
```

The mobile application communicates with the backend for authentication, nutrition tracking, workout tracking, water intake, and user-related operations.

The NutRise Android application has also been published to **Google Play**.

---

## ⚙️ Production Engineering Journey

NutRise started as a full-stack application and was later extended with production engineering practices.

```text
Backend Development
        ↓
Automated Testing
        ↓
Database Migrations
        ↓
Docker & Docker Compose
        ↓
GitHub Actions CI/CD
        ↓
Container Registry
        ↓
Security Scanning
        ↓
Railway Production Deployment
        ↓
Production Smoke Testing
        ↓
Asynchronous AI Reports (RabbitMQ + Python)
        ↓
Retry, DLQ & Manual Integration Checks
```

The goal of the project is not only to implement application features, but also to demonstrate how a backend service can be securely configured, tested, containerized, deployed, and verified through a repeatable engineering process.

---

## 👩‍💻 Author

**Meral Ateş**

Computer Engineering Graduate  
Backend Developer

Main areas of interest:

- Backend Development
- Java & Spring Boot
- REST APIs
- Database Systems
- Docker
- CI/CD
- Production Engineering
