# CodeProgress API v1.0.0

Release preparation date: 2026-10-24

CodeProgress API v1.0.0 is the first release candidate that provides authenticated, owner-scoped tracking of programming study topics, learning tasks, study sessions, and progress analytics.

## Highlights

- User registration with normalized unique email addresses and BCrypt password hashing.
- Stateless HS256 JWT login and authenticated `GET /api/users/me`.
- Per-user isolation for topics, tasks, study sessions, filters, progress, and dashboard queries.
- Topic, LearningTask, and StudySession CRUD with validation and consistent error responses.
- Task status, difficulty, priority, optional deadline, due-today, upcoming, and overdue views.
- Transactional reconciliation of completed task status when study sessions are created, updated, or deleted.
- Safe deletion rules that preserve recorded learning history.
- Pagination, allowlisted sorting, task filters, topic filters, and study-session date ranges.
- Dashboard summary and zero-filled daily activity analytics implemented with PostgreSQL aggregate queries.
- OpenAPI documentation and Swagger UI with Bearer authentication support.
- Docker Compose startup for one backend and PostgreSQL 17.
- Unit, MockMvc, security, and PostgreSQL Testcontainers coverage.

## Database schema

The release contains Flyway migrations V1–V10:

- V1–V3 create topics, learning tasks, study sessions, and their relation table.
- V4 tracks task statuses managed by study sessions.
- V5 protects learning history from unsafe cascading deletes.
- V6 adds query-performance indexes.
- V7 creates users.
- V8 adds topic ownership.
- V9 adds task deadline and priority planning fields.
- V10 rejects ownerless topics and makes `topics.owner_user_id` mandatory.

Back up an existing database before upgrade. V10 deliberately fails without modifying topic data if any topic still has no owner; it never assigns an arbitrary user or deletes data.

## Security model

- Learning endpoints require a valid Bearer access token.
- Topic ownership comes from the authenticated user and cannot be supplied by the client.
- Nested resources and analytics verify ownership through their topic.
- Password hashes, JWT secrets, SQL details, and stack traces are not returned through the API.
- JWT signing secrets must be supplied through configuration and contain at least 32 bytes.

## Verification completed during release preparation

- `mvn --batch-mode --no-transfer-progress clean verify`: successful.
- 162 unit/MockMvc/security tests: passed.
- 11 PostgreSQL 17 Testcontainers integration tests: passed.
- Total: 173 passed, 0 failed, 0 errors, 0 skipped.
- Flyway V1–V10 and Hibernate `ddl-auto=validate`: passed on isolated PostgreSQL 17.10.
- Docker image `codeprogress-backend:1.0.0`: built and started successfully in an isolated environment.
- Health, OpenAPI JSON, and Swagger UI checks: HTTP 200.
- The earlier end-to-end functional smoke scenario completed 24/24 HTTP requests successfully.

The GitHub Actions workflow has been checked locally, but an actual run in the target GitHub repository was not performed as part of this preparation and remains **NOT VERIFIED**.

## Configuration required for startup

At minimum, provide:

- `POSTGRES_PASSWORD` for Docker Compose;
- `JWT_SECRET` with at least 32 bytes;
- optional `JWT_ISSUER`, `JWT_ACCESS_TOKEN_TTL`, `APP_TIME_ZONE`, ports, and Swagger/logging settings as documented in `README.md`.

Do not commit `.env`, access tokens, real passwords, database dumps, or private keys.

## Known limitations

- There is no refresh-token or token-revocation workflow.
- Concurrent writes use last-write-wins semantics; entities do not have optimistic-locking versions.
- Password reset, email verification, roles, account deletion, and administrator endpoints are outside v1.0.0.
- Docker Compose is a single-instance development/release-verification setup, not a high-availability production platform.
- Swagger/OpenAPI is public by default and should be disabled with `SWAGGER_ENABLED=false` where public API documentation is inappropriate.

## Release operations not performed

No release commit, Git tag, Git push, GitHub Release, deployment, or publication was performed while preparing these notes.
