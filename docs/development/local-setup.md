# Local Development Setup

## Prerequisites

- Docker Desktop or Docker Engine
- Java 21
- Maven wrapper is already included in the backend project
- Node.js and npm for frontend work
- Git

## Required environment variable

The project intentionally fails fast if the database password is missing.

The real value never lives in the repo. Keep it outside, once:

```bash
printf 'DB_PASSWORD=%s\n' 'a-strong-local-password' > ~/.secrets/financial-gps.env
```

Then hand it to every compose command with `--env-file`:

```bash
docker compose --env-file ~/.secrets/financial-gps.env up --build
```

Forgetting the flag fails fast with `Set DB_PASSWORD before running compose`, which is the
intended behaviour. The repo `.env` only holds `DB_PASSWORD=change-me-locally`.

Exporting still works for a single shell when you run something outside compose:

```bash
export DB_PASSWORD='a-strong-local-password'
```

On PowerShell:

```powershell
$env:DB_PASSWORD = 'a-strong-local-password'
```

## Start services

### Full local stack with Docker Compose

```bash
docker compose --env-file ~/.secrets/financial-gps.env up --build
```

This starts:
- PostgreSQL on localhost:5434
- backend on the configured service port
- frontend on the configured service port

## Start backend manually

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Frontend development

```bash
cd frontend
npm install
npm run dev
```

## Run tests

All test lanes run in containers — no host Java, Node or PostgreSQL is needed. Full command table:
[AGENTS.md](../../AGENTS.md) → Test.

### Backend

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test
```

### Frontend unit tests

```bash
docker compose --env-file ~/.secrets/financial-gps.env run --rm --no-deps frontend npm run test:unit
```

### E2E suite

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile e2e up --exit-code-from e2e
```

## Important notes

- keep DB_PASSWORD set before running compose or local backend
- local Compose configuration uses dev-friendly settings and mounts
- regular docker compose up does not include the E2E profile unless explicitly requested
- if using a browser-based session flow, ensure cookie and CSRF settings match the local profile

## Common quick checks

- backend health: http://localhost:8080/actuator/health
- frontend: configured local UI URL from the project setup
- database: local Postgres instance on port 5434
