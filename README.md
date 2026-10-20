# E-Commerce Website

Fullstack e-commerce application: Java 21 + Spring Boot 4.1.x, React + Vite, PostgreSQL, Redis.

## Tech Stack

| Component | Version |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 4.1.x |
| PostgreSQL | 16 |
| Redis | 7 |
| Node.js | 22 LTS |
| React | 19 |
| Vite | latest |
| Ant Design | 6.x |
| Flyway | managed by Spring Boot BOM |
| Testcontainers | managed by Spring Boot BOM |

## Project Structure

```
.
├── Backend/          # Spring Boot backend (D-0-07: keep capital B)
├── frontend/         # React + Vite frontend
├── docs/             # Project documentation
├── docker-compose.yml
└── .env.example
```

## Prerequisites

- Java 21+
- Node.js 22 LTS
- Docker (for PostgreSQL 16 + Redis 7)

## Getting Started

### 1. Start infrastructure

```bash
docker compose up -d
```

Wait for both services to report `healthy` (`docker compose ps`).

### 2. Run backend

```bash
cd Backend
./mvnw spring-boot:run
```

Backend starts at `http://localhost:8080`.

- Health endpoint: `GET /api/v1/health`
- Actuator health: `GET /actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at `http://localhost:5173`.

## Testing

### Backend

```bash
cd Backend
./mvnw clean verify
```

Integration tests use Testcontainers (spins up PostgreSQL + Redis in Docker).

### Frontend

```bash
cd frontend
npm test
```

## Environment Variables

Copy `.env.example` to `.env` and adjust values. Default dev passwords are documented in `.env.example` (D-0-10). Never commit `.env`.

Phase 1 adds two **required** variables — the base `application.yml` declares them without defaults, so missing values fail fast outside the `local`/`test` profiles:

| Variable | Description |
|---|---|
| `JWT_SECRET` | HS256 signing secret, at least 32 bytes UTF-8 (shorter → app refuses to start). `application-local.yml` ships a dev-only default. |
| `ADMIN_PASSWORD_HASH` | BCrypt hash of the seeded admin password. `application-local.yml` and `application-test.yml` ship the hash of `Admin@123` for dev/test. |

**BCrypt hashes contain `$`** (e.g. `$2a$10$...`). Quote the value in single quotes (or escape as `$$` when Docker Compose interpolates it) so `$2a` is not expanded by the shell/Compose. See `.env.example`.

### Seeded admin account (dev/test)

- Email: `admin@ecommerce.dev`
- Password: `Admin@123`
- Role: `ADMIN` (Flyway `V3__seed_admin.sql`, applied on the first migrate against an empty database)

## Authentication (Phase 1)

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/auth/register` | Create user (role `USER`) |
| `POST /api/v1/auth/login` | Returns `accessToken` + `refreshToken` (JWT HS256) |
| `POST /api/v1/auth/refresh` | Rotate tokens (one-time refresh tokens, reuse → session revoked) |
| `POST /api/v1/auth/logout` | Revoke current session |
| `GET /api/v1/auth/me` | Current user profile |

Protected endpoints (e.g. `/api/v1/admin/**`, requires role `ADMIN`) need `Authorization: Bearer <accessToken>`. In Swagger UI click **Authorize** and paste the access token — the `bearerAuth` scheme is preconfigured.

## CI

GitHub Actions runs two jobs on push/PR to `main`/`develop`:
- **Backend**: `./mvnw clean verify` (working-directory: `Backend`)
- **Frontend**: `npm ci`, `npm run build`, `npm test` (working-directory: `frontend`)
