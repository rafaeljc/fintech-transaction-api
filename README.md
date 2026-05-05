# Fintech Transaction API

A REST API that accepts financial transactions and returns real-time statistics — built with Java 21 and Spring Boot as a hands-on way to learn the stack through a real challenge instead of tutorials.

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.5**
- **Maven**

## API

All endpoints are prefixed with `/api/v1`.

| Method   | Endpoint        | Description                                      |
|----------|-----------------|--------------------------------------------------|
| `POST`   | `/transactions` | Submit a transaction                             |
| `DELETE` | `/transactions` | Delete all transactions                          |
| `GET`    | `/statistics`   | Get statistics for the configured lookback duration |

### POST /transactions

```json
{
  "amount": 123.45,
  "dateTime": "2020-08-07T12:34:56.789-03:00"
}
```

| Status                     | Meaning                                                |
|----------------------------|--------------------------------------------------------|
| `201 Created`              | Transaction accepted and stored                        |
| `422 Unprocessable Entity` | Validation failed (future date, negative amount, etc.) |
| `400 Bad Request`          | Malformed request body                                 |

### GET /statistics

Returns aggregated statistics for transactions within the last 60 seconds.

```json
{
  "count": 10,
  "sum": 1234.56,
  "avg": 123.456,
  "min": 12.34,
  "max": 123.56
}
```

When no transactions exist in the window, all values are `0`.

## Getting Started

### Prerequisites

- Java 21+

### Build

```bash
./mvnw clean verify
```

This compiles the project, runs all tests, checks code coverage (≥ 80%), and runs style and static analysis checks.

### Run

```bash
./mvnw spring-boot:run
```

The API will be available at [`http://localhost:8080/api/v1`](http://localhost:8080/api/v1).

### Quick Start

Submit a transaction:

```bash
NOW=$(date -u +"%Y-%m-%dT%H:%M:%S.000+00:00")
curl -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 123.45, "dateTime": "'"$NOW"'"}'
```

Get statistics:

```bash
curl -s http://localhost:8080/api/v1/statistics | jq
```

Delete all transactions:

```bash
curl -i -X DELETE http://localhost:8080/api/v1/transactions
```

### Run Tests Only

```bash
./mvnw test
```

## Health Check

The management server runs on port **8081**, separate from the API.

```bash
curl -s http://localhost:8081/actuator/health | jq
```

**Docker Compose:**

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 3
```

## Configuration

| Env var | Default | Description |
|---|---|---|
| `STATISTICS_LOOKBACK_DURATION` | `60s` | How far back from now transactions are included in statistics. Must be greater than zero. Accepts `30s`, `2m`, `1h`, etc. |

**Local dev** — copy `.env.example` to `.env` and adjust values:

```bash
cp .env.example .env
```

```dotenv
STATISTICS_LOOKBACK_DURATION=30s
```

**Docker Compose:**

```yaml
services:
  api:
    image: fintech-transaction-api
    ports:
      - "8080:8080"
    env_file:
      - .env
```

## Project Structure

```
src/
├── main/java/io/github/rafaeljc/fintechtransactionapi/
│   ├── config/         # Configuration properties
│   ├── controller/     # REST endpoints
│   ├── service/        # Business logic
│   ├── store/          # Thread-safe in-memory data store
│   ├── dto/            # Request and response objects
│   ├── model/          # Domain model
│   ├── filter/         # MDC request logging filter
│   └── exception/      # Global exception handling
└── test/               # Mirrors main structure
```

## Documentation

- [`CHALLENGE.md`](CHALLENGE.md) — original challenge requirements
- [`docs/`](./docs) — implementation plan and endpoint specification
