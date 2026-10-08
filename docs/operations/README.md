# Operations Guide

## Scope

This section is for day-to-day operational knowledge: service startup, health validation, port tracking, and procedural recovery.

## Core operating principles

- keep environment variables explicit
- avoid silent defaults for critical settings like DB_PASSWORD
- document ports and ownership of runtime dependencies
- treat Compose and Kubernetes behavior as operational contracts
- keep troubleshooting notes in the repo, not only in terminal history

## Service map

| Service | Primary purpose | Default local port | Notes |
| --- | --- | ---: | --- |
| PostgreSQL | application database and session persistence | 5434 | required for local backend startup |
| Backend | API and business logic | 8080 | expects DB_PASSWORD |
| Frontend | browser UI | 4173 / dev port | depends on API availability |
| OpenCode tooling | local assistant/service runtime | configurable | avoid port collisions |

## Operational checklist

### Before starting the stack

- ensure Docker is running
- export DB_PASSWORD
- verify no conflicting process owns the expected ports

### After starting the stack

- confirm service status via Docker Compose
- confirm backend health endpoint responds
- confirm frontend can load without API errors
- check logs only if a service is unhealthy or fails to boot

## Related docs

- [ports-and-services.md](ports-and-services.md)
- [../runbooks/local-development.md](../runbooks/local-development.md)
- [../runbooks/docker-compose.md](../runbooks/docker-compose.md)
- [../troubleshooting/README.md](../troubleshooting/README.md)

## Standard rule

If an operational symptom is reproducible and worth keeping, it belongs in the docs set. Short-lived fixes should not become the only source of truth.
