# API Specification

## Base path

All endpoints are prefixed with `/api/v1`, configured via `server.servlet.context-path=/api/v1` in `application.yaml`.

---

## Endpoints

### POST /transactions

Receives and stores a transaction.

**Request body:**
```json
{
    "amount": 123.45,
    "dateTime": "2020-08-07T12:34:56.789-03:00"
}
```

| Field      | Type                          | Required |
|------------|-------------------------------|----------|
| `amount`   | Floating-point decimal        | Yes      |
| `dateTime` | ISO 8601 with timezone offset | Yes      |

**Validation (Bean Validation on DTO):**

| Rule                      | Annotation          |
|---------------------------|---------------------|
| `amount` not null         | `@NotNull`          |
| `dateTime` not null       | `@NotNull`          |
| `amount >= 0`             | `@PositiveOrZero`   |
| `dateTime` not in future  | `@PastOrPresent`    |

**Responses:**
- `201 Created` — transaction accepted, no body
- `422 Unprocessable Entity` — one or more validation rules failed (`MethodArgumentNotValidException`), no body
- `400 Bad Request` — request body is not parseable JSON (`HttpMessageNotReadableException`), no body

---

### DELETE /transaction

Deletes all stored transaction data.

**Responses:**
- `200 OK` — all data deleted, no body

---

### GET /statistics

Returns aggregated statistics for transactions that occurred in the **last 60 seconds**.

**Response body:**
```json
{
    "count": 10,
    "sum": 1234.56,
    "avg": 123.456,
    "min": 12.34,
    "max": 123.56
}
```

| Field   | Meaning                                       |
|---------|-----------------------------------------------|
| `count` | Number of transactions in the last 60 seconds |
| `sum`   | Total sum of amounts in the last 60 seconds   |
| `avg`   | Average amount in the last 60 seconds         |
| `min`   | Lowest amount in the last 60 seconds          |
| `max`   | Highest amount in the last 60 seconds         |

When no transactions exist in the last 60 seconds, all fields return `0`.

**Responses:**
- `200 OK` — statistics body always present

---

## Technical Constraints

- No database or cache systems — all data stored in memory
- Accept and respond with JSON only

---

## Data Store Design

### Structures

Three structures kept in sync at all times, all protected by a single `ReentrantReadWriteLock(true)` (fair mode):

1. `TreeMap<OffsetDateTime, List<BigDecimal>> window` — time-sorted window; used for O(log k) range eviction via `headMap(cutoff)`
2. `TreeMap<BigDecimal, Integer> amounts` — amount-to-frequency sorted multiset; `firstKey()` = min, `lastKey()` = max, both O(1)
3. `long count` and `BigDecimal sum` — running totals updated incrementally on every add and eviction

### Eviction

Called at the start of every write and before every read:

```
cutoff = OffsetDateTime.now().minusSeconds(60)
expired = window.headMap(cutoff)
for each (dateTime, amountList) in expired:
    for each amount in amountList:
        count--
        sum -= amount
        amounts.computeIfPresent(amount, (k, v) -> v == 1 ? null : v - 1)
expired.clear()
```

### Operation complexity

| Operation      | Complexity             | Notes                                       |
|----------------|------------------------|---------------------------------------------|
| `add()`        | O(log k) amortized     | k = transactions in the 60s window          |
| `statistics()` | O(1)                   | Reads pre-computed running values           |
| `clear()`      | O(1)                   | Replaces structures, resets counters        |
| Eviction       | O(log k + evicted)     | Each element evicted at most once           |

### Concurrency

`ReentrantReadWriteLock(true)` — **fair mode** — prevents writer starvation under asymmetric read/write load:

- **Write lock** — `add()`, `statistics()`, `clear()`: all three acquire exclusive access because eviction (which mutates shared state) is called at the start of both `add()` and `statistics()`
- Read lock is intentionally **not used**: since every operation evicts expired entries before reading, all operations require write access

Fair mode uses a FIFO queue: when a writer is waiting, new readers queue behind it instead of barging ahead. This guarantees writers (`POST /transactions`, `DELETE /transaction`) make progress even under heavy read load.
