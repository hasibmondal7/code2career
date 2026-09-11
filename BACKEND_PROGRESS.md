# Code2Career Backend Progress

## Current Status

The Code2Career backend has completed the core MVP and Phase 3 gamification scope.

- Java 17 and Spring Boot
- PostgreSQL with Spring Data JPA
- JWT authentication
- Flyway database migrations
- Docker-based code runner service
- Async code evaluation
- WebSocket submission status updates
- Admin authorization
- Rate limiting and request-size protection
- OpenAPI/Swagger documentation
- Health, metrics and correlation-ID logging

## Implemented API Areas

### Authentication and Users

- `POST /api/users/register`
- `POST /api/users/login`
- `PUT /api/users/password`
- `GET /api/users/me`
- `PATCH /api/users/me`
- `GET /api/users/me/activity`
- `GET /api/users/me/badges`

### Topics

- `GET /api/topics`
- `POST /api/topics` — Admin only

### Problems

- `GET /api/problems`
- `GET /api/problems/topic/{topicId}`
- `POST /api/problems` — Admin only

### Test Cases

- `GET /api/testcases/problem/{problemId}`
- `POST /api/testcases` — Admin only

### Code Submission and Evaluation

- `POST /api/submissions`
- `GET /api/submissions/user/{userId}`
- `POST /api/submissions/run`

The backend sends evaluation requests to the separate `code-runner` service. The runner executes Java code inside restricted Docker containers with:

- No network access
- CPU and memory limits
- Process limits
- Dropped Linux capabilities
- Read-only root filesystem
- Execution timeouts

### Gamification

- XP award on accepted submissions
- Automatic level calculation
- Daily streak tracking
- Daily activity history
- Duplicate XP protection
- Automatic badges:
  - `FIRST_CODE`
  - `SEVEN_DAY_STREAK`
  - `TEN_PROBLEMS`
  - `HUNDRED_XP`
- `GET /api/leaderboard`

## Security and Reliability

- JWT-based stateless authentication
- `USER` and `ADMIN` roles
- Admin-only content creation
- User-owned submission access protection
- Password change API
- Request body size limits
- Code input size limits
- Endpoint-based in-memory rate limiting
- Correlation ID response header: `X-Correlation-ID`
- Global structured API error responses
- Runner health check and request timeouts
- Pessimistic submission locking
- Duplicate XP and duplicate badge protection

## Database Migrations

Flyway migrations currently include:

- `V1__create_initial_schema.sql`
- `V2__add_submission_xp_awarded.sql`
- `V3__add_user_role.sql`
- `V4__normalize_user_role.sql`
- `V5__create_user_activity.sql`
- `V6__create_badges.sql`
- `V7__repair_user_role_data.sql`

## Documentation and Operations

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Application health: `http://localhost:8080/api/health`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator metrics: `http://localhost:8080/actuator/metrics`
- Docker Compose includes PostgreSQL, backend and code runner services.

Start the backend stack with:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

## Phase 3 Status

The core Phase 3 gamification requirements from the project README are complete:

- XP
- Levels
- Badges
- Achievements
- Streaks
- Leaderboard

The following leaderboard variations are future enhancements:

- Weekly leaderboard
- Monthly leaderboard
- Friends leaderboard
- College leaderboard
- Country leaderboard

## Testing

The backend includes:

- Mapper unit tests
- Leaderboard service tests
- Submission authorization tests
- Async duplicate-XP tests
- Security integration tests
- Spring application context tests

Run the backend tests with:

```powershell
Set-Location backend
.\mvnw.cmd test
```

## Known Follow-up Work

1. Add forgot-password and email-verification flows with SMTP.
2. Add weekly, monthly and friends leaderboards.
3. Add API integration tests for the code runner and database migrations.
4. Consider Redis-based distributed rate limiting for multiple backend instances.
5. Add monitoring dashboards and production backup procedures.
6. Begin Phase 4 with friends, PvP battles, private rooms and contests.

## Frontend Scope

The frontend is intentionally not included in this progress scope. Frontend integration will be implemented after the backend Phase 3 work is finalized.
