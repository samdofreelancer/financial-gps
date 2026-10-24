# AGENTS.md

**Index, not a manual.** Maps a task to the exact command or doc. Wrong or missing here?
Fix it in the same change that revealed the gap. Unconfirmed findings go to
[docs/development/agent-notes.md](docs/development/agent-notes.md).

## Stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 3.2, Java 21, single Maven module (`backend/`) |
| Frontend | Vue 3 + Pinia + Vue Router, Vite 5, Vitest (`frontend/`) |
| E2E | Playwright 1.63 + Allure (`e2e/`) |
| Database | PostgreSQL 16 |
| CI | GitHub Actions, own framework — see CI section |

Specs live in `specs/`, numbered by feature (`001-` … `010-`). Read the relevant
`specs/<feature>/spec.md` + `plan.md` before implementing a feature.

## Env

`~/.secrets/financial-gps.env` holds the one variable: `DB_PASSWORD`. It lives **outside the
repo**, and compose only sees it when you pass `--env-file`:

```bash
docker compose --env-file ~/.secrets/financial-gps.env up --build
```

The repo `.env` is just a gitignored placeholder (`DB_PASSWORD=change-me-locally`) — nothing
reads it, so no secret ever sits in the working tree (which is what keeps `cat`/`grep`/`xxd`
over the repo harmless). Nothing but compose sees the variable; a bare `./mvnw` or `npm` run
sees no env at all.

Only export it yourself when running something outside compose:

```bash
export DB_PASSWORD='...'   # PowerShell: $env:DB_PASSWORD='...'
```

## Test

**Two ways to run backend tests — pick ONE:**

### A. Local host (fastest, needs Java 21 + Postgres on localhost:5434)
```bash
export DB_PASSWORD='your-password'
cd backend && ./mvnw -B test                    # all tests
cd backend && ./mvnw -B test -Dtest=ClassName   # single class
cd backend && ./mvnw -B test -Dtest=ClassName#method  # single method
```

### B. Container (CI-parity, needs Docker, no host Java/Postgres)
```bash
# Uses compose.override.yaml (mounts source, named ~/.m2 volume)
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test

# Single class/method inside container
docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test ./mvnw -B test -Dtest=ClassName[#method]
```

> **Do NOT mix.** The container image (`compose.ci.yaml`) has no source mount and no `mvnw` — it's for CI only. Local dev uses `compose.override.yaml` which mounts source and provides `mvnw`.

| Task | Command |
|---|---|
| **Backend — all tests (local, recommended)** | `cd backend && ./mvnw -B test` |
| Backend, single class / method (local) | `cd backend && ./mvnw -B test -Dtest=ClassName[#method]` |
| Backend — all tests (container, CI-parity) | `docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test` |
| Backend, single class (container) | `docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test ./mvnw -B test -Dtest=ClassName[#method]` |
| Frontend unit | `cd frontend && npm run test:unit` |
| Frontend unit, watch | `cd frontend && npm run test:unit:watch` |
| E2E (stack must be up) | `docker compose --env-file ~/.secrets/financial-gps.env --profile e2e up --exit-code-from e2e` |
| E2E headed / debug | `cd e2e && npm run test:headed` / `test:debug` |
| E2E report | `cd e2e && npm run allure:generate && npm run allure:open` |
| E2E serial (CI stability, avoids backend contention) | `cd e2e && npx playwright test --workers=1` |
| CI framework conformance | `bash .github/actions/guard-framework/scripts/guard.sh` |
| CI schema | `actionlint` (pinned 1.7.7, install once) |

The container needs Docker — Testcontainers boots its own PostgreSQL through the mounted socket. Without Docker the suite fails, it does not skip.

**Windows line endings:** if `./mvnw` fails in container with "cannot open maven-wrapper.properties", the wrapper script has `\r`. Fix once:
```bash
sed -i 's/\r$//' backend/mvnw backend/mvnw.cmd
git add backend/mvnw backend/mvnw.cmd
```

## Run

Container by default; host only for the Vite dev server, which has no secret and wants HMR.

| Task | Command |
|---|---|
| Full stack (backend + frontend + postgres) | `docker compose --env-file ~/.secrets/financial-gps.env up --build` |
| Postgres only | `docker compose --env-file ~/.secrets/financial-gps.env up -d` |
| Frontend dev server | `cd frontend && npm run dev` |

`local` profile sets `cookie.secure=false` so login works over HTTP. CI uses
`compose.ci.yaml` explicitly (no source mounts) — plain `docker compose` never loads it.

## Gotchas

1. **`npm run test:e2e` in `frontend/` is not the E2E runner.** It shells out to `../e2e`.
   The real suite is in `e2e/` and needs a running stack; override origin with
   `E2E_BASE_URL` (default `http://127.0.0.1:4173`).

2. **`backend-test` container image (CI) has NO `mvnw` and NO source mount.**
   - `compose.ci.yaml` builds a minimal image for CI — no wrapper, no source.
   - Local dev uses `compose.override.yaml` which mounts source + provides `mvnw`.
   - If `docker compose --profile test run backend-test` fails with "mvnw not found", you're using the wrong compose file or the image wasn't built with source.

3. **Windows line endings (`\r`) break `mvnw` in Linux container.**
   Fix once and commit:
   ```bash
   sed -i 's/\r$//' backend/mvnw backend/mvnw.cmd
   git add backend/mvnw backend/mvnw.cmd
   ```

4. **E2E parallel runs cause backend contention** — 15 tests hit the same backend simultaneously.
   Use serial for CI stability:
   ```bash
   cd e2e && npx playwright test --workers=1
   ```

5. **Domain lane is framework-free.** `com.financialgps.domain` must not import Spring,
   JPA, auth or JDBC. `application` must not import Spring, JPA, `infrastructure`, `api`
   or `platform`. `DomainBoundaryGuardTest` + `ArchitectureGuardTest` fail the build, but
   their messages point at the import, not the rule.

6. **Transactions are applied at the input-port boundary**, in
   `infrastructure.configuration.UseCaseTransactions` — not in the use case, not in the
   controller.

7. **Controllers own no financial rule and no clock.** `LocalDate` comes from the
   `BusinessDate` port, never `LocalDate.now()`.

8. **Cross-owner access returns `404 RESOURCE_NOT_FOUND`, never `403`.** Deliberate
   resource hiding, not a bug to fix.

## CI

```text
ci.yml (triggers + fan-out)  →  reusable-*.yml (one context each)
                              →  actions/*/action.yml (tool setup)
                              →  actions/*/scripts/*.sh (the real logic)
```

| To change | Edit |
|---|---|
| Add a CI check | new `reusable-<name>.yml` + one job block in `ci.yml` |
| Which paths trigger a job | `filters:` in `reusable-changes.yml` |
| What a job runs | `actions/<tool>/scripts/*.sh` |
| A compose operation | `actions/compose-run/scripts/compose.sh` |
| The advisory PR review (model, prompt, skip rules) | `reusable-ai-review.yml` + `actions/opencode-review/` + `.opencode/agents/review.md` |
| API contract / auth behavior | specs, then a new ADR in `docs/decisions/` |

Guard-enforced: `ci.yml` takes `uses:`/`needs:`/`if:`/`name:` only — no `run:`, no
`timeout-minutes` (forbidden alongside `uses:`; timeouts go inside each reusable's job).
Each `reusable-*` is `workflow_call` and nothing else, with a `timeout-minutes` per job and
permissions covered by the `ci.yml` ceiling. Scripts use `github.action_path`, never
`github.workspace`. Third-party `uses:` pinned to full SHAs with a `# vN` comment. A job
whose `if:` gate is false reports **Success** so required checks stay green. Full rule
table: [.github/README.md](.github/README.md).

## Doc map

| Question | File |
|---|---|
| Local setup, env vars, running the stack | [docs/development/local-setup.md](docs/development/local-setup.md) |
| Which tests for what change | [docs/development/testing.md](docs/development/testing.md) |
| Contribution / review process | [docs/development/contributing.md](docs/development/contributing.md) |
| Unconfirmed findings, dead ends | [docs/development/agent-notes.md](docs/development/agent-notes.md) |
| Backend DDD package map | [docs/architecture/overview.md](docs/architecture/overview.md) |
| HTTP API, errors, status codes | [docs/architecture/api-contract.md](docs/architecture/api-contract.md) |
| Domain calculation rules | [docs/architecture/business-domain.md](docs/architecture/business-domain.md) |
| Auth, session, CSRF, ownership | [docs/architecture/security.md](docs/architecture/security.md) |
| Why a decision was made | [docs/decisions/](docs/decisions/) |
| Deploy / troubleshoot k8s | [k8s/README.md](k8s/README.md) |
| Local troubleshooting | [docs/troubleshooting/](docs/troubleshooting/) |

## Working agreement

- Read the spec and the doc above before implementing; don't infer intent from code alone.
- Verify with the narrowest test that proves the change, then widen before finishing.
- **Hit a trap not covered here?** Log it in `agent-notes.md` with the capture template
  there — symptom verbatim, not cause. Seen it twice and it still holds? Promote it into
  Gotchas above and delete the note.
- Don't commit secrets. `.env` is gitignored; use `.env.example` as the template.