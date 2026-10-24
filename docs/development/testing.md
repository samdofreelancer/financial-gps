# Testing Strategy

## Goals

The project uses tests to verify the business rules, security boundaries, and user flows that matter most. The testing strategy should protect both correctness and the architectural constraints of the codebase.

## Testing layers

### 1. Unit tests

Use unit tests for isolated domain and application logic where framework concerns are intentionally absent.

Focus on:

- domain rules
- business calculations and policy checks
- use case behavior under valid and invalid conditions

### 2. MVC / API contract tests

Use backend tests to validate HTTP, authentication flow, validation rules, and standardized problem responses.

These tests should confirm:

- protected endpoints require auth
- invalid credentials return the appropriate problem
- CSRF protection blocks invalid requests
- ownership enforcement is applied consistently
- failure responses match the expected contract

### 3. Integration tests

The backend uses PostgreSQL-backed integration tests with Testcontainers for realistic data and persistence flows.

This layer matters because data integrity and ownership rules are not fully validated by isolated logic tests alone.

### 4. End-to-end tests

The Playwright suite validates real browser-driven flows, including registration, login, session persistence, protected access, export, and logout.

This is the highest-confidence proof that the full user flow works with real browser behavior and server-side session handling.

## Required validation before merge

For a change in auth, security, or user-owned data, the minimum review bar should include:

- relevant backend tests
- relevant E2E or browser flow validation when the interaction is user-facing
- documentation update when the behavior or setup changes

## Non-functional expectations

The project treats architecture as part of the contract. Tests and review should confirm:

- domain rules remain framework-free
- application boundaries remain respected
- no security assumptions are weakened silently
- changes to auth/session logic do not regress browser flows

## Practical commands

All three lanes run in containers (see `AGENTS.md` → Test): none needs host Java, Node or Postgres.

### Backend

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test

# single class / method
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test \
  ./mvnw -B test -Dtest=ClassName[#method]
```

On the Windows dev box Docker lives inside WSL, so wrap the same command:
`wsl -d Ubuntu -- bash -lc 'cd /mnt/d/Working/financial-gps && DB_PASSWORD=local-test docker compose --profile test run --rm backend-test'`.

### Frontend unit tests

```bash
docker compose --env-file ~/.secrets/financial-gps.env run --rm --no-deps frontend npm run test:unit
docker compose --env-file ~/.secrets/financial-gps.env run --rm --no-deps frontend npm run build
```

`--no-deps` keeps `run` from starting and possibly recreating the stack with a different
`DB_PASSWORD`; see `agent-notes.md`. The `frontend` service needs its `frontend-node-modules`
volume populated — start the stack once, or prefix `npm install && `.

### E2E validation

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile e2e up --exit-code-from e2e
```

## Definition of done

A feature or fix is only considered ready when:

- the impacted behavior is verified by the relevant tests
- the user-facing flow has been exercised where appropriate
- the architecture and security constraints remain intact
- the accompanying docs reflect the final behavior

## Related docs

- [local-setup.md](local-setup.md)
- [contributing.md](contributing.md)
- [../architecture/security.md](../architecture/security.md)
