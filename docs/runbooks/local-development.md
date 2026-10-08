# Local Development Runbook

## Purpose

This runbook is the default entry point for developers working on the project locally. It covers the standard startup sequence, validation steps, and common recovery actions.

## Preconditions

- Docker Desktop or Docker Engine running
- Java 21 available on the machine
- Node.js and npm installed for frontend work
- Git configured
- a local DB password set in the environment

## Required environment variable

The backend and Compose stack intentionally fail fast if DB_PASSWORD is missing.

### Bash / zsh

```bash
export DB_PASSWORD='a-strong-local-password'
```

### PowerShell

```powershell
$env:DB_PASSWORD = 'a-strong-local-password'
```

## Start the full stack

From the repository root:

```bash
export DB_PASSWORD='a-strong-local-password'
docker compose up --build
```

This starts the shared services needed by the app, including PostgreSQL and the application containers.

## Start backend only

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Use the local profile for browser-friendly session settings while developing on HTTP.

## Start frontend only

```bash
cd frontend
npm install
npm run dev
```

## Health checks

After startup, validate the critical services:

- backend: http://localhost:8080/actuator/health
- frontend: configured UI URL from local dev settings
- database: PostgreSQL on localhost:5434

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

### E2E verification

```bash
DB_PASSWORD='a-strong-local-password' docker compose --profile e2e up --exit-code-from e2e
```

## Recovery steps

### If Docker services do not start

1. confirm Docker is running
2. confirm DB_PASSWORD is exported in the current shell
3. review compose logs

```bash
docker compose logs --tail=200
```

### If port conflicts occur

Check the port and release the conflicting process. See the port troubleshooting guide and the port-kill script if available in the repo root or local tooling docs.

### If backend refuses to boot

1. confirm PostgreSQL is healthy
2. confirm the database password is not empty
3. check the application log output for datasource or migration errors

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Exit / reset

To stop local stack services:

```bash
docker compose down
```

To fully reset the local data volume:

```bash
docker compose down -v
```

## Keep this runbook current

When working on the project, update this guide if a dependency, port, or startup step changes. Local setup docs are part of the project’s durable knowledge base.
