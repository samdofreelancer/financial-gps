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
docker compose --env-file ~/.secrets/financial-gps.env up -d
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test
docker compose --env-file ~/.secrets/financial-gps.env run --rm --no-deps frontend npm run build
```

Tests run in containers only; see [AGENTS.md](../../AGENTS.md) → Test for the full table.

`--no-deps` keeps `run` from starting or recreating the stack with a different `DB_PASSWORD`,
and the `frontend` service needs its `frontend-node-modules` volume populated — start the
stack once, or prefix `npm install && `.

## Responsibilities of a new contributor

- read the architecture overview
- understand the deployment topology
- review the latest runbook and troubleshooting notes
- avoid changing configuration without updating docs
