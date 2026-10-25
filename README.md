# CodeProgress API

[![CI](https://github.com/alexweher/code-progress-api/actions/workflows/ci.yml/badge.svg)](https://github.com/alexweher/code-progress-api/actions/workflows/ci.yml)

Current release: **v1.0.0**

CodeProgress is a Spring Boot REST API for organizing programming study. It tracks learning topics, planned tasks, study sessions, completion status, deadlines, priorities, and owner-scoped progress analytics.

Every learning record belongs to an authenticated user. Requests cannot read or modify topics, tasks, sessions, or statistics owned by another user.

## Technology stack

- Java 21
- Spring Boot 3.5
- Spring Web and Jakarta Validation
- Spring Data JPA and Hibernate
- Spring Security with stateless HS256 JWT authentication
- PostgreSQL 17 for the Docker Compose environment
- Flyway database migrations
- springdoc-openapi and Swagger UI
- JUnit 5, Mockito, MockMvc, and Testcontainers
- Maven
- Docker and Docker Compose

## Architecture

The project uses a package-by-feature structure. HTTP requests generally follow this flow:

```text
Controller -> Service -> Repository -> PostgreSQL
```

- Controllers handle HTTP mapping, request validation, and response status codes.
- Services enforce ownership, business rules, and transaction boundaries.
- Spring Data repositories contain persistence and aggregate queries.
- DTOs define API request and response contracts; JPA entities are not exposed directly.
- Flyway owns schema evolution, while Hibernate uses `ddl-auto: validate`.
- The `common` package contains cross-cutting configuration, pagination, and error handling.

## Features

- User registration and login with normalized unique email addresses.
- Password hashing through Spring Security `PasswordEncoder`.
- Stateless JWT access tokens and `GET /api/users/me`.
- Owner-scoped CRUD operations for topics and learning tasks.
- Topic and task states: `PLANNED`, `IN_PROGRESS`, and `COMPLETED`.
- Task difficulty (`EASY`, `MEDIUM`, `HARD`), priority (`LOW`, `MEDIUM`, `HIGH`), and optional deadline.
- Task search by topic, status, difficulty, priority, and deadline range.
- Due-today, inclusive seven-day upcoming, and overdue task views.
- Study sessions with duration, date, note, and completed task references.
- Transactional reconciliation of task status when study sessions are created, updated, or deleted.
- Safe deletion rules that preserve study history.
- Paginated and sorted search endpoints with a maximum page size of 100.
- Topic progress, dashboard summary, and daily activity analytics.
- Daily activity ranges include zero-activity days and are limited to 366 days.
- Per-user data isolation for CRUD, filters, and aggregate queries.

## Requirements

For Docker Compose:

- Docker Desktop with Docker Compose
- Git Bash on Windows for the commands below

For a local JVM run:

- JDK 21
- Maven 3.8.5 or newer
- PostgreSQL compatible with the existing Flyway migrations

## Docker Compose startup

Copy the environment template and replace both placeholder secrets:

```bash
cp .env.example .env
notepad.exe .env
```

Start PostgreSQL and the API:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

The backend waits for the PostgreSQL healthcheck before starting. Flyway applies pending migrations during Spring Boot startup, before Hibernate validates the schema.

### Database migrations

Version 1.0.0 contains Flyway migrations V1–V10. They create the topic, task, study-session, and user schema; protect learning history; add ownership, planning fields, and query indexes; and finally require every topic to have a valid owner. Do not edit migrations that have already been applied. Existing installations should be backed up and validated before applying pending migrations.

Check the API:

```bash
curl http://localhost:8080/api/health
```

Inspect applied migrations:

```bash
docker compose exec postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Stop containers without deleting PostgreSQL data:

```bash
docker compose down
```

Do not add `-v` unless permanent deletion of the named PostgreSQL volume is intentional.

## Environment variables

| Variable | Required | Default | Purpose |
|---|---:|---|---|
| `POSTGRES_DB` | Docker Compose | `codeprogress` | PostgreSQL database name |
| `POSTGRES_USER` | Docker Compose | `codeprogress` | PostgreSQL user |
| `POSTGRES_PASSWORD` | Yes in Compose | none | PostgreSQL password |
| `POSTGRES_HOST_PORT` | No | `5433` | PostgreSQL port exposed to the host |
| `DB_URL` | Local JVM only | `jdbc:postgresql://127.0.0.1:5433/codeprogress` | JDBC URL; Compose supplies `postgres:5432` automatically |
| `DB_USERNAME` | Local JVM only | `codeprogress` | JDBC username |
| `DB_PASSWORD` | Local JVM only | `codeprogress` | JDBC password |
| `BACKEND_HOST_PORT` | No | `8080` | Backend port exposed by Compose |
| `SERVER_PORT` | No | `8080` | Spring Boot server port |
| `JWT_SECRET` | Yes | none | HS256 signing secret; must contain at least 32 bytes |
| `JWT_ISSUER` | No | `codeprogress-api` | Expected JWT issuer |
| `JWT_ACCESS_TOKEN_TTL` | No | `PT1H` | Access-token lifetime as an ISO-8601 duration |
| `SWAGGER_ENABLED` | No | `true` | Enables or disables both Swagger UI and OpenAPI JSON |
| `APP_TIME_ZONE` | No | `UTC` | Calendar time zone for deadlines and analytics |
| `LOG_LEVEL` | No | `INFO` | Root log level in Docker Compose |

Never commit `.env`, database passwords, JWT secrets, or access tokens. The repository contains only `.env.example` placeholders.

## Running without Docker for the backend

Start PostgreSQL from Compose:

```bash
docker compose up -d postgres
```

Export a development JWT secret and run Spring Boot:

```bash
export JWT_SECRET='replace-with-a-local-random-secret-at-least-32-bytes'
mvn spring-boot:run
```

The default JDBC settings connect to `127.0.0.1:5433`.

## Authentication

Registration and login are public. All learning data, user profile, and dashboard endpoints require an access token.

Register:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "student@example.com",
    "password": "replace-with-a-strong-password"
  }'
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "student@example.com",
    "password": "replace-with-a-strong-password"
  }'
```

The response contains an access token:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Pass it in the `Authorization` header:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

The API does not currently issue refresh tokens.

## REST examples

Create a topic:

```bash
curl -X POST http://localhost:8080/api/topics \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Java concurrency",
    "description": "Threads, executors and synchronization"
  }'
```

Create a task with planning information:

```bash
curl -X POST http://localhost:8080/api/topics/1/tasks \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Study CompletableFuture",
    "description": "Build a small asynchronous pipeline",
    "difficulty": "MEDIUM",
    "deadline": "2026-10-25",
    "priority": "HIGH"
  }'
```

Search tasks:

```bash
curl 'http://localhost:8080/api/tasks/search?status=IN_PROGRESS&priority=HIGH&page=0&size=20&sort=deadline,asc' \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Record a study session:

```bash
curl -X POST http://localhost:8080/api/topics/1/sessions \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionDate": "2026-10-20",
    "durationMinutes": 45,
    "note": "Practiced asynchronous composition",
    "completedTaskIds": [1]
  }'
```

Get dashboard activity:

```bash
curl 'http://localhost:8080/api/dashboard/activity?from=2026-10-14&to=2026-10-20' \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Main endpoint groups

| Group | Paths |
|---|---|
| Health | `GET /api/health` |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login` |
| User | `GET /api/users/me` |
| Topics | `/api/topics`, `/api/topics/search`, `/api/topics/{id}`, `/api/topics/{topicId}/progress` |
| Tasks | `/api/topics/{topicId}/tasks`, `/api/tasks/{id}`, `/api/tasks/search`, `/api/tasks/due-today`, `/api/tasks/upcoming`, `/api/tasks/overdue` |
| Sessions | `/api/topics/{topicId}/sessions`, `/api/sessions/{id}`, `/api/sessions/search` |
| Dashboard | `/api/dashboard/summary`, `/api/dashboard/activity` |

## Errors

Expected failures use a consistent JSON structure. Validation errors additionally populate `fieldErrors`:

```json
{
  "timestamp": "2026-10-20T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/topics",
  "fieldErrors": [
    {
      "field": "title",
      "message": "Title is required"
    }
  ]
}
```

Common statuses are `400` for invalid input, `401` for missing or invalid authentication, `404` for missing or non-owned resources, `409` for state conflicts, and `500` for unexpected server failures. Server responses do not expose stack traces, SQL, or secrets.

## OpenAPI and Swagger UI

After startup:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger UI and the OpenAPI document are public so the API can be studied before login. Protected operations still require JWT authentication. Use **Authorize** and enter the access token; Swagger adds the `Bearer` prefix.

Set `SWAGGER_ENABLED=false` to remove both documentation endpoints in an environment where public API documentation is not desired.

## Tests

Run unit and MockMvc tests without Docker:

```bash
mvn test
```

Run the complete verification lifecycle, including the real PostgreSQL Testcontainers suite:

```bash
mvn verify
```

`mvn verify` requires a running Docker Engine. Testcontainers creates an isolated PostgreSQL instance, applies the real Flyway migrations, and does not use the local development database.

## Continuous integration

The GitHub Actions workflow in `.github/workflows/ci.yml` runs on pushes to `main` and on pull requests. It uses Java 21 and executes:

```bash
mvn --batch-mode --no-transfer-progress verify
```

This command builds the application, runs unit and MockMvc tests, and runs PostgreSQL integration tests through Testcontainers on the GitHub-hosted Docker Engine.

## Version 1.0.0 limitations

- Access tokens have no refresh-token or revocation workflow.
- Concurrent updates of the same record use last-write-wins semantics; entities do not yet use optimistic locking with `@Version`.
- The API does not provide account deletion, password reset, email verification, roles, or administrator endpoints.
- Swagger/OpenAPI is public by default for learning and local development; disable it with `SWAGGER_ENABLED=false` where public documentation is not appropriate.
- Docker Compose is intended for a single backend instance and PostgreSQL. It is not a high-availability production deployment.
- The GitHub Actions workflow must still be confirmed by an actual run in the target GitHub repository; local YAML and command checks cannot replace that run.

See [RELEASE_NOTES_v1.0.0.md](RELEASE_NOTES_v1.0.0.md) for the release summary and known constraints.
