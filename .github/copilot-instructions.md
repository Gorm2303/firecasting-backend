# Copilot instructions – firecasting-backend

## Project layout
- Spring Boot 3.4.x + Java 23, Maven multi-module structure
- **Runnable module:** `application/` (only module with Spring Boot plugin enabled; parent sets `<skip>true</skip>`)
- **Key modules:** `domain-model`, `simulation*`, `persistence`, `common-*`, `phase-*`, etc.
- Build tool: Maven (via `mvnw.cmd` on Windows)

## Build & run
- **Build all:** `./mvnw.cmd -f pom.xml -DskipTests package`
- **Run app:** `./mvnw.cmd -pl application -am spring-boot:run`
  - Requires local Postgres or `SPRING_PROFILES_ACTIVE=local` (disables DB)
- **Docker:** Dockerfile builds with `mvn -DskipTests -f firecasting/pom.xml package` then runs JRE 23

## Core API routes (FirecastingController)
- `POST /api/simulation/start` — start simple simulation (returns `{id}` or dedup hit)
- `POST /api/simulation/start-advanced` — start advanced simulation (form-driven)
- `GET /api/simulation/progress/{simulationId}` — SSE stream (events: `queued`, `started`, `progress`, `completed`, `heartbeat`)
- `GET /api/simulation/progress/{simulationId}` — also responds with JSON if already completed
- `GET /api/simulation/queue/{simulationId}` — task info (status, position)
- `GET /api/simulation/export` — CSV of last results
- `GET /api/forms/{formId}` — fetch JSON schema (e.g., `advanced-simulation`)

## Critical flow: simulation start → dedup → queue → SSE → DB
1. **SimulationStartService.startSimulation()** receives request
   - Calls `StatisticsService.findExistingRunIdForInput()` (SHA-256 hash of canonical JSON)
   - If dedup hit → return existing id immediately (202 Accepted)
   - If miss → new UUID, enqueue task
2. **SimulationQueueService.submitWithId()** queues runnable
   - Idempotent per simulationId (returns true if already known)
   - Returns 429 if executor pool is full
   - Tracks queue position via `shadowQueue` (0-based)
3. **SSE progress stream** (`/api/simulation/progress/{id}`)
   - Frontend connects via EventSource, receives:
     - `queued` event: `"position:N"` (0-based) or plain `"queued"`
     - `started` event: `"running"`
     - `progress` events: human-readable strings (e.g., "Completed 100/1000 runs")
     - `heartbeat` event: keep-alive tick
     - `completed` event: JSON array of `YearlySummary` objects
4. **SimulationSseService** manages emitter lifecycle
   - Priority queue (heartbeat < progress < state)
   - Flusher runs at `settings.sse-interval` ms (default 1000)
5. **Persistence** via `StatisticsService` (append-only)
   - Parent `SimulationRunEntity`: id, inputHash, inputJson, createdAt
   - Children `YearlySummaryEntity`: phase, year, columns, percentile grids (101 values)
   - Schema managed by Flyway

## Profiles
- **`!local`** (default, active if not explicitly set to `local`):
  - Enables controller, StatisticsService, Postgres/JPA/Flyway
  - See `application.yml`
- **`local`**:
  - Disables datasource, JPA, Flyway, controller
  - Used for unit tests or in-memory simulation testing
  - See `application-local.yml`

## Error handling
- **ApiExceptionHandler** returns shape: `{ message: String, details: String[] }`
- Frontend parses `details` as field errors; backend validates via `@Valid` + Spring Validation

## Key dependencies
- Spring Boot starter-web, starter-actuator, starter-security, starter-validation
- Spring Data JPA + Hibernate
- PostgreSQL JDBC driver + Flyway
- Micrometer (Prometheus metrics)
- Lombok
- Spring Modulith (modular architecture)
- JUnit 5, Mockito

## Configuration (environment variables)
- `SPRING_PROFILES_ACTIVE=prod|dev|local`
- `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD`
- `SPRING_DATASOURCE_HIKARI_*` (pool tuning)
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (safety)
- `SPRING_FLYWAY_ENABLED=true`
- `SETTINGS_RUNS=10000`, `SETTINGS_BATCH_SIZE=500`, `SETTINGS_TIMEOUT=600000`
- `SETTINGS_SSE_INTERVAL=1000` (ms between SSE flushes)
- `SIMULATION_PROGRESSSTEP=1000` (runs per progress update)
- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus`
