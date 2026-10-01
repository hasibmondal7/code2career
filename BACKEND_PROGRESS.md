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
- Initial Phase 4 PvP private-room lobby APIs

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

The backend sends evaluation requests to the separate `code-runner` service. The runner executes Java 17, C++17, Python 3, and JavaScript inside restricted Docker containers with:

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

### PvP Rooms (initial Phase 4 slice)

- `POST /api/pvp/rooms` — Create a private friend-invite or public matchmaking room
- `GET /api/pvp/rooms/open` — List public rooms waiting for an opponent
- `POST /api/pvp/rooms/{inviteCode}/join` — Accept/join a waiting room
- `GET /api/pvp/rooms/{inviteCode}` — View a room as a member
- `POST /api/pvp/rooms/{inviteCode}/start` — Host starts the room
- `POST /api/pvp/rooms/{inviteCode}/submit` — Evaluate battle code and settle the first accepted solution
- Room membership and lifecycle updates are persisted and broadcast to `/topic/pvp/rooms/{inviteCode}`.
- Accounts start with 100 virtual coins. Both players stake coins before a match starts; insufficient balance blocks entry. The first accepted solution wins the pot and earns difficulty-based XP. Expired matches with no winner refund the stakes.
- PvP supports the four code-runner languages: Java 17, C++17, Python 3 and JavaScript.

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

Flyway migrations cover the initial schema, user roles and gamification, profile and problem updates, roadmaps, and PvP rooms. The latest migration is `V17__add_pvp_battle_stakes.sql`.

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
6. Add PvP multi-language support, ranked matchmaking, disconnect/reconnect grace rules and broader integration tests.

## Frontend Scope

The frontend includes friend invite rooms, public matchmaking acceptance, live lobby updates, a battle editor/countdown, and coin/XP result display. The server controls evaluation and settlement; coins are virtual and have no real-money value.

### Two-phone local PvP smoke test

1. Restart/rebuild the backend so Flyway applies migration V17 and the PvP endpoints are loaded. Rebuild the code-runner image as well.
2. Find the development computer's LAN IPv4 address. Set `CORS_ALLOWED_ORIGIN` to both `http://localhost:5173` and `http://<LAN-IP>:5173`, and set the frontend `VITE_API_URL` to `http://<LAN-IP>:8080`.
3. Start the frontend with `npm run dev -- --host 0.0.0.0`; allow ports 5173 and 8080 through the computer's firewall on the private network.
4. Connect both phones to the same Wi-Fi and open `http://<LAN-IP>:5173`. Sign in with two separate accounts.
5. On phone one, create a friend-invite room and share its code, or create a public room. On phone two, join by code or accept the public match. Choose the same problem/difficulty and language before starting.
6. Both accounts need enough virtual coins for the stake. The winner receives the pot and XP; a timed-out match with no accepted result returns both stakes.

PvP coins have no real-money value and cannot be purchased or withdrawn.
