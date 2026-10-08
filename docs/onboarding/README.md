# Onboarding

This guide helps a new contributor understand how to start working on the repository.

## Recommended checklist

- clone the repository
- install prerequisites
- start local services with Docker Compose
- run backend tests
- run frontend checks
- inspect architecture docs
- read the latest ADRs and troubleshooting notes

## Common commands

```bash
docker compose up -d
./backend/mvnw test
cd frontend && npm install && npm run build
```

## Responsibilities of a new contributor

- read the architecture overview
- understand the deployment topology
- review the latest runbook and troubleshooting notes
- avoid changing configuration without updating docs
