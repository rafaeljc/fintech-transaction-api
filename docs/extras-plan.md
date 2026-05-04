# Extras Execution Plan

Branch: `feat/extras`

Improvements to the base implementation covering configuration, performance observability, health check, containerization, and API documentation.

---

## Task 1 — Configurable statistics window duration

**Goal:** Replace the hardcoded 60-second window in `TransactionStore` with a validated, environment-configurable property.

**Files:**
- `src/main/java/.../config/StatisticsProperties.java` — new
- `src/main/java/.../FintechTransactionApiApplication.java` — add `@EnableConfigurationProperties`
- `src/main/java/.../store/TransactionStore.java` — inject and use `StatisticsProperties`
- `.env.example` — new
- `.gitignore` — add `.env`
- `README.md` — document configuration

**Steps:**

1. Create `config/StatisticsProperties.java`:
   - `@Validated`
   - `@ConfigurationProperties(prefix = "statistics")`
   - Record field: `@DefaultValue("60s") @NotNull Duration windowDuration`
   - Compact constructor: reject zero or negative durations with `IllegalArgumentException` including the received value in the message

2. Add `@EnableConfigurationProperties(StatisticsProperties.class)` to `FintechTransactionApiApplication`

3. Inject `StatisticsProperties` into `TransactionStore` via constructor; store as `private final` field; replace `minusSeconds(60)` with `minus(properties.windowDuration())` in `evict()`

4. Create `.env.example`:
   ```
   # Must be positive. Accepts units: 30s, 2m, 1h. Default: 60s
   STATISTICS_WINDOW_DURATION=60s
   ```

5. Add `.env` to `.gitignore`

6. Update `README.md` with a "Configuration" section documenting `STATISTICS_WINDOW_DURATION` and usage examples for local dev, Docker, and Kubernetes

**Tests (`StatisticsPropertiesTest.java`):**
- Default value is 60 seconds when property is not set
- Custom valid value (e.g. 30s) is accepted
- Zero duration throws `IllegalArgumentException` at startup
- Negative duration throws `IllegalArgumentException` at startup

---

## Task 2 — Statistics calculation performance logging

**Goal:** Log the elapsed time of each statistics calculation as a structured field, visible in the existing JSON logs.

**Files:**
- `src/main/java/.../service/StatisticsService.java` — add timing
- `src/test/java/.../service/StatisticsServiceTest.java` — assert log emission

**Steps:**

1. In `StatisticsService.get()`:
   - Capture `long start = System.nanoTime()` before calling `store.statistics()`
   - Capture `long end = System.nanoTime()` after the call
   - Compute `long durationMicros = (end - start) / 1_000`
   - `LOG.debug("Statistics calculated: durationMicros={}", durationMicros)`
   - Return the response unchanged

**Tests:**
- Assert the log line is emitted using Logback `ListAppender`
- Assert `durationMicros` is non-negative

---

## Task 3 — Health check endpoint

**Goal:** Expose `GET /api/v1/health` returning `{"status":"UP"}`, suitable for use as a liveness probe in container orchestration platforms.

**Files:**
- `pom.xml` — add `spring-boot-starter-actuator`
- `src/main/resources/application.yaml` — configure management endpoints
- `src/test/java/.../integration/HealthCheckIT.java` — new
- `README.md` — document health check

**Steps:**

1. Add to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```

2. Add to `application.yaml`:
   ```yaml
   management:
     server:
       port: 8081
     endpoints:
       web:
         base-path: /
         exposure:
           include: health
     endpoint:
       health:
         show-details: never
   ```
   The management server runs on port 8081, separate from the API on port 8080. This keeps operational endpoints out of the `/api/v1` context path, making the health check available at `http://localhost:8081/health`.

3. Update `README.md` with the health endpoint URL (`http://localhost:8081/health`) and examples for Docker Compose and Kubernetes probe configuration

**Tests (`HealthCheckIT.java`):**
- `GET /health` on port 8081 returns `200 OK`
- Response body contains `{"status":"UP"}`

---

## Task 4 — Containerization

**Goal:** Package the application as a minimal, production-grade Docker image using a multi-stage build. Document use of the `/health` endpoint for external health probing.

**Files:**
- `Dockerfile` — new
- `.dockerignore` — new
- `README.md` — add Docker section

**Steps:**

1. Create `Dockerfile` with two stages:

   **Stage 1 (`builder`) — `eclipse-temurin:21-jdk-alpine`:**
   - `WORKDIR /build`
   - Copy `pom.xml`, `.mvn/`, `mvnw`
   - `RUN chmod +x mvnw && ./mvnw dependency:go-offline` (cache layer)
   - Copy `src/`
   - `RUN ./mvnw package -DskipTests`

   **Stage 2 (`runtime`) — `gcr.io/distroless/java21-debian12`:**
   - `WORKDIR /app`
   - Copy fat JAR from builder: `COPY --from=builder /build/target/*.jar app.jar`
   - `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`
   - No `HEALTHCHECK` instruction — distroless has no shell or HTTP client; health probing is handled externally using `GET /health` on the management port (8081)

2. Create `.dockerignore`:
   ```
   .git
   .gitignore
   .idea
   target/
   *.iml
   .env
   docs/
   README.md
   CHALLENGE.md
   ```

3. Update `README.md` with:
   - `docker build -t fintech-transaction-api .`
   - `docker run -p 8080:8080 fintech-transaction-api`
   - Env var override: `docker run -e STATISTICS_WINDOW_DURATION=30s ...`
   - Expose both ports: `-p 8080:8080 -p 8081:8081`
   - Docker Compose `healthcheck` example using `GET /health` on port 8081
   - Kubernetes liveness/readiness probe example using `GET /health` on port 8081

---

## Task 5 — API documentation

**Goal:** Provide an accurate, hand-written OpenAPI 3.0 specification served as a static resource, with Swagger UI available at `/docs`.

**Files:**
- `src/main/resources/static/openapi.yaml` — new
- `pom.xml` — add `springdoc-openapi-starter-webmvc-ui`
- `src/main/resources/application.yaml` — configure springdoc
- `README.md` — link to `/docs`

**Steps:**

1. Write `src/main/resources/static/openapi.yaml`:
   - OpenAPI version: `3.0.3`
   - `info`: title, version, description
   - `servers`: `url: /api/v1`
   - `paths`:
     - `POST /transactions` — request body (`amount`: number, minimum 0, required; `dateTime`: string, format date-time, required, must not be future); responses: 201, 400, 422
     - `DELETE /transactions` — response: 200
     - `GET /statistics` — response: 200 with `StatisticsResponse` schema (`count`, `sum`, `avg`, `min`, `max` all number, minimum 0)
   - `components/schemas`: `TransactionRequest`, `StatisticsResponse`

2. Add to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.8.8</version>
   </dependency>
   ```

3. Add to `application.yaml`:
   ```yaml
   springdoc:
     swagger-ui:
       path: /docs
       url: /openapi.yaml
     api-docs:
       enabled: false
   ```

4. Update `README.md` with a link to `http://localhost:8080/api/v1/docs`
