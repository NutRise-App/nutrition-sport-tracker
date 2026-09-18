# NutRise AI Report Messaging Contract

## RabbitMQ topology

Exchange: `nutrise.ai.exchange` (direct, durable)

| Routing key           | Queue                             | Publisher   | Consumer    |
| --------------------- | --------------------------------- | ----------- | ----------- |
| `ai.report.requested` | `nutrise.ai.report.request.queue` | Spring Boot | Python      |
| `ai.report.generated` | `nutrise.ai.report.result.queue`  | Python      | Spring Boot |

Messages use JSON with camelCase field names and `application/json` content type.

## Report request

Spring Boot publishes a message matching the Python `AiReportRequest` model:

```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 42,
  "periodStart": "2026-09-07",
  "periodEnd": "2026-09-13",
  "loggedDays": 5,
  "weeklyStats": {
    "averageDailyCaloriesIn": 1850,
    "averageDailyCaloriesBurned": 350,
    "averageDailyWaterMl": 1750,
    "totalWorkoutMinutes": 180
  }
}
```

These are fictional example values, not real user data.

## Report result

Python publishes a message matching `AiReportResponse`:

```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 42,
  "summary": "Haftanın genel özeti.",
  "nutritionAnalysis": "Beslenme kayıtlarının değerlendirmesi.",
  "workoutAnalysis": "Egzersiz kayıtlarının değerlendirmesi.",
  "waterAnalysis": "Su tüketimi kayıtlarının değerlendirmesi.",
  "recommendations": ["Öneri bir.", "Öneri iki."]
}
```

## Processing rules

- `reportId` identifies the same report in both messages.
- Spring Boot owns database access, report persistence and email delivery.
- Python generates the report; it does not send email or access the application database.
- The publisher should mark messages as persistent.
- Consumers should acknowledge a message only after completing the required processing.
- Retry, failed-report messages and dead-letter handling will be designed separately.
