# Local Development Setup

## Prerequisites

- Docker Desktop or Docker Engine
- Java 21
- Maven wrapper is already included in the backend project
- Node.js and npm for frontend work
- Git

## Required environment variable

The project intentionally fails fast if the database password is missing.

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
export DB_PASSWORD='a-strong-local-password'
docker compose up --build
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

### Backend

```bash
cd backend
./mvnw test
```

### Frontend unit tests

```bash
cd frontend
npm run test:unit
```

### E2E suite

```bash
DB_PASSWORD='a-strong-local-password' docker compose --profile e2e up --exit-code-from e2e
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
