# Docker Compose Runbook

## Purpose

This document explains how the project uses Docker Compose for local development, test execution, and CI-oriented validation.

## Files in use

- `compose.yaml` — base service definitions and shared contracts
- `compose.override.yaml` — local developer overrides, mounts, and hot-reload settings
- `compose.ci.yaml` — CI-safe stack with explicit service orchestration

## Base startup

```bash
export DB_PASSWORD='a-strong-local-password'
docker compose up -d
```

This is the default developer entry point for starting the application dependencies and service containers.

## Local override behavior

The project expects Docker Compose to automatically apply the local override file for developer-friendly behavior. The override file is intended for:

- source mounts
- local build settings
- development-friendly service configuration
- hot reload or dev-oriented container behavior

## CI behavior

For CI or deterministic validation, Compose is run with an explicit file combination:

```bash
DB_PASSWORD='ci-password' docker compose \
  -f compose.yaml -f compose.ci.yaml \
  --profile e2e up --build --abort-on-container-exit --exit-code-from e2e
```

This intentionally avoids local source mounting and keeps the runtime closer to the intended CI environment.

## E2E profile

The E2E suite is gated behind a profile and is not started by default during regular local `docker compose up`.

```bash
DB_PASSWORD='a-strong-local-password' docker compose --profile e2e up --exit-code-from e2e
```

## Useful commands

### List running services

```bash
docker compose ps
```

### Inspect logs

```bash
docker compose logs --tail=200
```

### Stop services

```bash
docker compose down
```

### Reset volumes

```bash
docker compose down -v
```

## Common issues

### Missing DB_PASSWORD

The backend and PostgreSQL configuration intentionally fail fast when the environment variable is absent. This is expected, not a bug.

### Port already in use

Use a free local port or stop the conflicting service. This is a common local issue when a previous process already bound the target port.

### Containers remain unhealthy

Check:

- Docker daemon status
- DB_PASSWORD value
- startup logs for database migration or service wiring errors

## Maintenance note

Whenever a service port, image, dependency, or environment contract changes, update the Compose file plus the operational docs in the project knowledge base.
