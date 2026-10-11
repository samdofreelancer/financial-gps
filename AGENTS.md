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

`.env` in the repo root holds one key: `DB_PASSWORD`. It is gitignored, and **`docker
compose` reads it automatically**. Nothing else does — a bare `./mvnw` or `npm` run sees
no env at all.

Only export it yourself when running something outside compose:

```bash
export DB_PASSWORD='...'   # PowerShell: $env:DB_PASSWORD='...'
```

## Test

**Run backend tests in the container.** It needs no `JAVA_HOME`, no `DB_PASSWORD`, and no
host Maven — the image is the same JDK 21 + Maven 3.9.9 CI uses, sources are mounted, and
`~/.m2` is a named volume, so repeat runs are warm.

| Task | Command |
|---|---|
| **Backend — all tests (default)** | `docker compose --profile test run --rm backend-test` |
| Backend, single class / method | `docker compose --profile test run --rm backend-test ./mvnw -B test -Dtest=ClassName[#method]` |
| Frontend unit | `cd frontend && npm run test:unit` |
| Frontend unit, watch | `cd frontend && npm run test:unit:watch` |
| E2E (stack must be up) | `docker compose --profile e2e up --exit-code-from e2e` |
| E2E headed / debug | `cd e2e && npm run test:headed` / `test:debug` |
| E2E report | `cd e2e && npm run allure:generate && npm run allure:open` |
| CI framework conformance | `bash .github/actions/guard-framework/scripts/guard.sh` |
| CI schema | `actionlint` (pinned 1.7.7, install once) |

The container needs Docker — Testcontainers boots its own PostgreSQL through the mounted
socket. Without Docker the suite fails, it does not skip.

**Do not run `./mvnw` on the host.** It is slower (cold `~/.m2`, different Maven than CI),
and on Windows it tempts a `JAVA_HOME` export plus a hand-grepped `DB_PASSWORD` from
`.env` — compose already does both for you. If a task seems to need it, that is a signal
the command is wrong, not that the shell needs setting up.

## Run

Container by default; host only for the Vite dev server, which has no secret and wants HMR.

| Task | Command |
|---|---|
| Full stack (backend + frontend + postgres) | `docker compose up --build` |
| Postgres only | `docker compose up -d` |
| Frontend dev server | `cd frontend && npm run dev` |

`local` profile sets `cookie.secure=false` so login works over HTTP. CI uses
`compose.ci.yaml` explicitly (no source mounts) — plain `docker compose` never loads it.

## Gotchas

1. **`npm run test:e2e` in `frontend/` is not the E2E runner.** It shells out to `../e2e`.
   The real suite is in `e2e/` and needs a running stack; override origin with
   `E2E_BASE_URL` (default `http://127.0.0.1:4173`).
2. **Domain lane is framework-free.** `com.financialgps.domain` must not import Spring,
   JPA, auth or JDBC. `application` must not import Spring, JPA, `infrastructure`, `api`
   or `platform`. `DomainBoundaryGuardTest` + `ArchitectureGuardTest` fail the build, but
   their messages point at the import, not the rule.
3. **Transactions are applied at the input-port boundary**, in
   `infrastructure.configuration.UseCaseTransactions` — not in the use case, not in the
   controller.
4. **Controllers own no financial rule and no clock.** `LocalDate` comes from the
   `BusinessDate` port, never `LocalDate.now()`.
5. **Cross-owner access returns `404 RESOURCE_NOT_FOUND`, never `403`.** Deliberate
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
