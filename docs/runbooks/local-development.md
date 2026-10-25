# Local Development Runbook

## Purpose

This runbook is the default entry point for developers working on the project locally. It covers the standard startup sequence, validation steps, and common recovery actions.

## Preconditions

- Docker Desktop or Docker Engine running
- Java 21 available on the machine
- Node.js and npm installed for frontend work
- Git configured
- the local DB password stored outside the repo in `~/.secrets/financial-gps.env`

## Required secret file

The backend and Compose stack intentionally fail fast if `DB_PASSWORD` is missing. Keep the
real value outside the repo, once:

```bash
printf 'DB_PASSWORD=%s\n' 'a-strong-local-password' > ~/.secrets/financial-gps.env
```

Then hand it to every compose command with `--env-file`. Exporting the variable still works
for a single shell when running something outside compose:

```bash
export DB_PASSWORD='a-strong-local-password'
```

On PowerShell:

```powershell
$env:DB_PASSWORD = 'a-strong-local-password'
```

## Start the full stack

From the repository root:

```bash
docker compose --env-file ~/.secrets/financial-gps.env up --build
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

All test lanes run in containers — no host Java, Node or PostgreSQL is needed
([AGENTS.md](../../AGENTS.md) → Test).

### Backend

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test
```

### Frontend unit tests

```bash
docker compose --env-file ~/.secrets/financial-gps.env run --rm --no-deps frontend npm run test:unit
```

### E2E verification

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile e2e up --exit-code-from e2e
```

## Recovery steps

### If Docker services do not start

1. confirm Docker is running
2. confirm `~/.secrets/financial-gps.env` exists and every compose command passes
   `--env-file ~/.secrets/financial-gps.env`
3. review compose logs

```bash
docker compose --env-file ~/.secrets/financial-gps.env logs --tail=200
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
docker compose --env-file ~/.secrets/financial-gps.env down
```

To fully reset the local data volume:

```bash
docker compose --env-file ~/.secrets/financial-gps.env down -v
```

## Keep this runbook current

When working on the project, update this guide if a dependency, port, or startup step changes. Local setup docs are part of the project’s durable knowledge base.
