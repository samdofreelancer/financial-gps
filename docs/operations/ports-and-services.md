# Services and Ports

## Default local ports

| Service | Port | Notes |
| --- | ---: | --- |
| PostgreSQL | 5434 | Local database instance |
| Backend | 8080 | Spring Boot API |
| Frontend | 4173 / dev port depending on setup | Browser-facing UI |
| OpenCode service | configurable | avoid port conflicts with local tooling |

## Service responsibilities

### PostgreSQL

- stores application relational data and session state
- supports local development and integration tests
- health-checked by Compose

### Backend

- serves API endpoints
- enforces security and session policies
- handles domain use cases and persistence

### Frontend

- serves the SPA and user interactions
- depends on the backend service for API behavior

## Port conflict handling

If a local tool reports the port is already in use:

```bash
lsof -i :49374
```

or

```bash
netstat -ano | findstr :49374
```

Then either stop the conflicting process or move the service to a free port.

Example:

```bash
opencode service set port 50000
opencode
```

## Verification

After startup, validate that the expected listeners are active:

```bash
docker compose ps
```

and review health endpoints or logs when needed.
