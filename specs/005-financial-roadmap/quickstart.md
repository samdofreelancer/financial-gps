# Quickstart: Financial Roadmap (005)

**Date**: 2026-10-22
**Purpose**: runnable validation scenarios that prove the feature works end to end.

## Prerequisites

| Requirement | Check |
|---|---|
| Java 21 + Maven wrapper | `java -version` → 21; `backend/mvnw` exists |
| Node 20+ | `node -v` |
| PostgreSQL reachable at `localhost:5434` (local run) **or** Docker (container run) | `docker compose ps` |
| `DB_PASSWORD` available | Container: `~/.secrets/financial-gps.env` exists. Local host: `echo $DB_PASSWORD` (PowerShell: `$env:DB_PASSWORD`) |

`DB_PASSWORD` lives **outside the repo** in `~/.secrets/financial-gps.env`, and compose only sees it
when you pass `--env-file ~/.secrets/financial-gps.env` (AGENTS.md → Env). The gitignored `.env` in
the repository root is only a placeholder (`DB_PASSWORD=change-me-locally`) and nothing reads it. A
bare `./mvnw` or `npm run` sees no environment — export the variable yourself when running outside
compose (PowerShell: `$env:DB_PASSWORD='...'`).

## 1. Backend — unit and contract tests

```bash
export DB_PASSWORD='your-password'
cd backend && ./mvnw -B test -Dtest='*Roadmap*Test'
```

The selector `*Roadmap*Test` matches `RoadmapGeneratorTest`, `RoadmapStageDetailsTest`,
`GetRoadmapUseCaseTest`, `RoadmapControllerTest`, `RoadmapStageDetailsApiTest` and
`RoadmapOwnershipIsolationTest`.

Container (CI parity):

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test \
  ./mvnw -B test -Dtest='*Roadmap*Test'
```

**Expected**: the roadmap domain, application and API tests pass; the architecture guard stays green
(the roadmap domain package imports nothing but `domain` and the JDK).

## 2. Backend — full suite (regression)

```bash
cd backend && ./mvnw -B test
```

**Expected**: no regression against the 001-004 suites (record the actual count when you run it;
the suite is the source of truth, not this file).

## 3. Run the stack and read the roadmap by hand

```bash
docker compose --env-file ~/.secrets/financial-gps.env up --build   # backend + frontend + postgres
```

Register an account, seed a profile, one debt and two goals, then:

```bash
curl -s -c /tmp/cookies.txt -X POST http://127.0.0.1:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"roadmap@example.com","password":"Str0ng!Passw0rd"}' -i
```

Copy the `XSRF-TOKEN` cookie, then call the roadmap:

```bash
curl -s -b /tmp/cookies.txt -H "X-XSRF-TOKEN: $XSRF" \
  'http://127.0.0.1:8080/api/v1/roadmap?asOf=2026-10-22'
```

**Expected**: a JSON document with `currency`, `routeStatus`, `currentStageOrder`,
`orderPolicy.policy = DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY`, and one stage per debt and per
non-archived, non-`DEBT_FREEDOM` goal — the debt stage first.

Repeat the call with the same `asOf`: the response must be byte-identical (SC-004, RM-013).

Time one read against the plan's performance goal (95% under 1 second for an owner with 50 debts
and 25 active goals) and record the result in `tasks.md` (T046).

## 4. Frontend — unit tests

```bash
cd frontend && npm run test:unit
```

**Expected**: the roadmap store/component tests pass alongside the existing suite.

## 5. End-to-end journey

```bash
docker compose --env-file ~/.secrets/financial-gps.env --profile e2e up --exit-code-from e2e
```

Single spec (needs the stack up):

```bash
cd e2e && npx playwright test specs/financial-roadmap.spec.ts --workers=1
```

**Expected**: the journey registers an account, seeds a profile/debt/goals, opens the roadmap
screen, sees the ordered stages with the current stage highlighted, opens a stage detail, and sees
the blocking explanation.

## 6. Manual verification against the verification matrix

| Case | How to reproduce | Expected |
|---|---|---|
| RM-001 | One goal, no debt | One `GOAL` stage, `status = CURRENT` |
| RM-002 | One debt + two goals | Debt stage first, then goals by `(priority, createdAt, id)` |
| RM-003 | `EMERGENCY_FUND` goal with priority 5 + another goal with priority 1 | Emergency-fund goal precedes the other goal |
| RM-004 | Complete the debt and every goal | `routeStatus = COMPLETED`, `currentStageOrder = null` |
| RM-005 | Fresh account, no facts; then only a `DEBT_FREEDOM` goal, no debt | `routeStatus = NO_DESTINATION`, `stages = []` in both variants |
| RM-006 | Complete a goal while the debt is still open | The goal stage is `UPCOMING` with `blockedBy = 1` |
| RM-007 | Goal without a target date | `requiredMonthlyCapacity = null` + `UNDATED_GOAL` wording in the route explanation |
| RM-008 | Income = expense (capacity 0) | ETA `null` + `NO_AVAILABLE_CAPACITY`; no fabricated date |
| RM-009 | Register without a Financial Profile | `MISSING_FINANCIAL_PROFILE` in blockers/explanations; no capacity value |
| RM-010 | Two goals, same priority, at least one `createdAt = null` (domain test only — the API always stamps `createdAt`) | The undated goal precedes the dated one; `id` breaks ties; order stable across reads |
| RM-011 | Two goals, same priority, different `createdAt` | Earlier `createdAt` first |
| RM-012 | Archive a goal | No stage for the archived goal; the rest of the route is unchanged |
| RM-013 | Two identical reads | Byte-identical response |
| RM-014 | Two owners; read each roadmap, then `GET /goals/{id}` for the other owner's goal | Each roadmap holds only its owner's goals; the foreign goal read is `404 RESOURCE_NOT_FOUND` |

## 7. Determinism guard

```bash
cd backend && ./mvnw -B test -Dtest=RoadmapGeneratorTest
```

**Expected**: the same facts and `asOf` produce equal stage orders, statuses and reason codes.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `404` on `/api/v1/roadmap` | Route not registered / wrong context path | Check `api/roadmap/RoadmapController` `@RequestMapping("/api/v1/roadmap")` |
| `400 VALIDATION_FAILED` on `asOf` | Malformed date | Use `YYYY-MM-DD` |
| `401` | Session cookie or CSRF header missing | Log in first; send `X-XSRF-TOKEN` on mutations (this route is a GET) |
| Stage order changes between reads | A fact changed between the calls (goal created/updated) | Read both times without mutating in between |
| E2E fails only in parallel | Backend contention | `npx playwright test --workers=1` |
