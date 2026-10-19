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
| Ant Design | 5.x |
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

## CI

GitHub Actions runs two jobs on push/PR to `main`/`develop`:
- **Backend**: `./mvnw clean verify` (working-directory: `Backend`)
- **Frontend**: `npm ci`, `npm run build`, `npm test` (working-directory: `frontend`)
