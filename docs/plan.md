# Execution Plan

## Package structure

```
io.github.rafaeljc.fintechtransactionapi
  controller/
    TransactionController.java
    StatisticsController.java
  dto/
    TransactionRequest.java
    StatisticsResponse.java
  exception/
    GlobalExceptionHandler.java
  filter/
    MdcFilter.java
  model/
    Transaction.java
  service/
    TransactionService.java
    StatisticsService.java
  store/
    TransactionStore.java
```

---

## Tasks

### Task 0 — Logging infrastructure

- Add `logstash-logback-encoder` dependency to `pom.xml`
- Create `src/main/resources/logback-spring.xml` with a JSON console appender using `LogstashEncoder`
  - Include MDC keys: `requestId`, `method`, `uri`, `status`
- Create `MdcFilter` (`@Component`, extends `OncePerRequestFilter`):
  - On each request: `MDC.put("requestId", UUID.randomUUID().toString())`, `method`, `uri`
  - In `finally`: `MDC.clear()`
- Log at `INFO` level on transaction accepted and statistics retrieved; `WARN` on validation rejection

---

### Task 1 — `model/Transaction.java`

Internal domain value object.

- Record with `BigDecimal amount` and `OffsetDateTime dateTime`
- No validation annotations — constructed only after validation passes; valid by invariant

---

### Task 2 — `dto/TransactionRequest.java`

Boundary DTO for `POST /transactions`.

- Record with `BigDecimal amount` and `OffsetDateTime dateTime`
- Bean Validation annotations:
  - `@NotNull` on `amount` and `dateTime`
  - `@PositiveOrZero` on `amount`
  - `@PastOrPresent` on `dateTime`

---

### Task 3 — `dto/StatisticsResponse.java`

Response DTO for `GET /statistics`.

- Record with `long count`, `BigDecimal sum`, `BigDecimal avg`, `BigDecimal min`, `BigDecimal max`
- Static factory `empty()` returning all-zero instance for when no transactions exist in the window

---

### Task 4 — `store/TransactionStore.java`

In-memory store. Maintains three structures under a `ReentrantReadWriteLock(true)` (fair mode):

- `TreeMap<OffsetDateTime, List<BigDecimal>> window` — time-sorted, for eviction
- `TreeMap<BigDecimal, Integer> amounts` — sorted multiset (amount → frequency), for O(1) min/max
- `long count` and `BigDecimal sum` — running totals

**Methods:**

`void add(Transaction t)` — write lock:
1. Evict expired entries
2. Append `t.amount()` to `window.computeIfAbsent(t.dateTime(), ...)`
3. `count++`, `sum += t.amount()`
4. `amounts.merge(t.amount(), 1, Integer::sum)`

`StatisticsResponse statistics()` — write lock:
1. Evict expired entries
2. If `count == 0` return `StatisticsResponse.empty()`
3. Return `new StatisticsResponse(count, sum, sum/count, amounts.firstKey(), amounts.lastKey())`

> **Note:** `statistics()` must hold a **write lock**, not a read lock, because eviction mutates `window`, `amounts`, `count`, and `sum`. Mutating shared state under a read lock is a data race when multiple threads call `GET /statistics` concurrently.

`void clear()` — write lock:
1. Assign new `TreeMap` instances to `window` and `amounts`
2. Reset `count = 0`, `sum = BigDecimal.ZERO`

**Eviction** (private, called within the write lock already held):
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

> `computeIfPresent` with a `null` return value atomically removes the key when the frequency reaches zero, avoiding the transient `0`-value entry that `merge` + `remove` would leave between two lines.

---

### Task 5 — `exception/GlobalExceptionHandler.java`

- `@RestControllerAdvice`
- `HttpMessageNotReadableException` → `400 Bad Request`, no body
- `MethodArgumentNotValidException` → `422 Unprocessable Entity`, no body

---

### Task 6 — `service/TransactionService.java`

- Constructor-injects `TransactionStore`
- `void add(TransactionRequest request)`:
  - Maps `TransactionRequest` → `Transaction`
  - Calls `store.add(transaction)`
- `void clear()`: delegates to `store.clear()`

---

### Task 7 — `service/StatisticsService.java`

- Constructor-injects `TransactionStore`
- `StatisticsResponse get()`: delegates to `store.statistics()`

---

### Task 8 — `controller/TransactionController.java`

- `POST /transactions`:
  - Parameter: `@Valid @RequestBody TransactionRequest`
  - Calls `transactionService.add(request)`
  - Returns `201 Created`, no body
- `DELETE /transaction`:
  - Calls `transactionService.clear()`
  - Returns `200 OK`, no body

---

### Task 9 — `controller/StatisticsController.java`

- `GET /statistics`:
  - Calls `statisticsService.get()`
  - Returns `200 OK` with `StatisticsResponse` body

---

### Task 10 — `application.yaml`

```yaml
server:
  servlet:
    context-path: /api/v1
```

---

### Task 11 — Unit tests: `TransactionStoreTest.java`

- `statistics()` returns all zeros when store is empty
- `statistics()` returns correct count/sum/avg/min/max for transactions within window
- `statistics()` excludes transactions older than 60 seconds
- `statistics()` returns zeros after `clear()`
- Adding multiple transactions with same amount updates frequency correctly
- Eviction removes amount key from `amounts` when frequency reaches zero
- Concurrent adds do not corrupt state (multi-threaded test)
- Concurrent `add` + `statistics` calls do not corrupt state
- `clear` while concurrent `statistics` calls are in progress leaves store in clean empty state

---

### Task 12 — Unit tests: `TransactionServiceTest.java`

- `add()` stores transaction when all fields are valid
- `add()` stores transaction when `amount` is zero

---

### Task 13 — Integration tests: `TransactionControllerTest.java`

- `POST /transactions` returns `201` for valid transaction
- `POST /transactions` returns `422` for null `amount`
- `POST /transactions` returns `422` for null `dateTime`
- `POST /transactions` returns `422` for future `dateTime`
- `POST /transactions` returns `422` for negative `amount`
- `POST /transactions` returns `201` for zero `amount`
- `POST /transactions` returns `400` for malformed JSON
- `DELETE /transaction` returns `200`
- After `DELETE /transaction`, `GET /statistics` returns all zeros

---

### Task 14 — Integration tests: `StatisticsControllerTest.java`

- `GET /statistics` returns `200` with all-zero body when store is empty
- `GET /statistics` returns correct values after inserting transactions
- `GET /statistics` excludes transactions outside the 60-second window
- Response JSON contains exactly the fields: `count`, `sum`, `avg`, `min`, `max`
