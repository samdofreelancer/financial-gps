# Deployment Architecture

## Summary

The project supports a layered deployment model built around local development, Docker Compose orchestration, and Kubernetes-oriented production-style manifests.

## Local deployment model

The repository root defines the shared local runtime contracts through Compose files:

- `compose.yaml` — shared service definitions
- `compose.override.yaml` — local developer-oriented overrides
- `compose.ci.yaml` — CI-focused runtime configuration

### Runtime components

- PostgreSQL database on port 5434
- backend API on the configured backend port
- frontend application on its UI port
- optional E2E runner behind a feature profile

## Required environment config

The system is designed to fail fast when critical config is absent.

Required variable:

```bash
DB_PASSWORD
```

This value must be present before starting Compose or running the backend locally. Missing values should be treated as a configuration error, not as a fallback condition.

## Local startup flow

### Start core stack

```bash
export DB_PASSWORD='a-strong-local-password'
docker compose up --build
```

### Start backend manually

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Start frontend locally

```bash
cd frontend
npm install
npm run dev
```

## E2E mode

The Playwright suite is profile-gated and is not part of the default Compose stack.

```bash
DB_PASSWORD='a-strong-local-password' docker compose --profile e2e up --exit-code-from e2e
```

This keeps the default local developer experience lighter while still enabling full end-to-end validation.

## Kubernetes model

Production-style manifests live under `k8s/` and model a full stack deployment pattern:

- PostgreSQL as a backing data store
- backend service exposed through the app runtime
- frontend service exposing browser access
- secret-based configuration rather than hardcoded credentials

The dev overlay is intended for local cluster scenarios while the base config represents the production-like configuration boundary.

## Deployment checklists

### Before deployment

- confirm DB_PASSWORD is supplied from a secret or environment manager
- verify the correct runtime profile for secure cookies
- validate that the network/services topology matches the target environment
- verify app startup health and dependency readiness

### After deployment

- check service health endpoints
- confirm session behavior under HTTPS when required
- validate database connectivity and migrations
- verify the frontend can reach the backend as intended

## Operational notes

- keep port assumptions explicit in docs
- avoid silent default values for environment-critical settings
- maintain parity between local, CI, and deployment behavior where relevant
- document any change in the runtime topology or service port

## Related docs

- [security.md](security.md)
- [overview.md](overview.md)
- [../operations/ports-and-services.md](../operations/ports-and-services.md)
- [../runbooks/docker-compose.md](../runbooks/docker-compose.md)
