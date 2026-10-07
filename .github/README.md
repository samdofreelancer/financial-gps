# GitHub Actions framework

Thin orchestrator → reusable workflows → composite actions → bash scripts.
Each layer has one job; logic flows inward, configuration flows outward as
typed inputs/env.

```text
.github/workflows/ci.yml                  composition root (triggers + fan-out, no run: steps)
.github/workflows/reusable-*.yml          use cases, one bounded context each (workflow_call)
.github/actions/*/action.yml              adapters (tool setup, no business logic)
.github/actions/*/scripts/*.sh            private scripts, co-located with their only consumer
.github/scripts/common/*.sh               shared helpers — only when 2+ actions need them (YAGNI until then)
```

Ownership rule: a script used by one action lives inside that action's folder
(portable via `github.action_path`, versioned together). A top-level
`scripts/` folder implies shared ownership — reserve it for genuinely shared
helpers (`common/`), don't create it speculatively.

Naming: `reusable-` prefix marks callable partials (never directly triggered).
Named by role ("reusable unit"), not by mechanism (`workflow_call` trigger) —
`wc-` would rot if the trigger were renamed, and `wc` already means word-count.
`ci.yml` is the only entry point.

## Architecture (current)

File tree:

```text
.github/
├── README.md                              this framework doc
├── workflows/
│   ├── ci.yml                             orchestrator (triggers + fan-out, no run: steps)
│   ├── reusable-changes.yml               use case: path filter → backend/frontend/e2e flags
│   ├── reusable-backend.yml               use case: containerized mvn test
│   ├── reusable-frontend.yml              use case: vitest suite
│   ├── reusable-e2e.yml                   use case: compose stack + Playwright + artifacts
│   └── reusable-guards.yml                use case: framework conformance (runs first, ungated)
└── actions/
    ├── compose-run/
    │   ├── action.yml                     adapter: one compose operation, env-injected
    │   └── scripts/compose.sh             private logic (config|backend-test|down-test|e2e-up|down-e2e)
    ├── guard-framework/
    │   ├── action.yml                     adapter: conformance check entry point
    │   └── scripts/guard.sh               the six rules below (grep/awk only, zero deps)
    └── setup-node-deps/action.yml         adapter: setup-node + npm ci
```

Call flow on push/PR:

```text
push/PR
  └─▶ ci.yml ──▶ guards ──▶ reusable-guards ──▶ guard-framework ──▶ scripts/guard.sh
                  │
                  └─▶ (only if guards pass) reusable-changes ──┬─▶ [if backend|e2e]  reusable-backend ──▶ compose-run ──▶ scripts/compose.sh
                                                               ├─▶ [if frontend|e2e] reusable-frontend ──▶ setup-node-deps
                                                               └─▶ [if any]          reusable-e2e ──────▶ compose-run ──▶ scripts/compose.sh
```

Layer rules:

| Layer | Lives in | May contain | Must not contain |
|---|---|---|---|
| Composition root | `ci.yml` | `on:`, `needs:`, `if:` gates, `uses:` | `run:` steps |
| Use cases | `reusable-*.yml` | one bounded context, typed `inputs`/`outputs` | inline shell logic, cross-context steps |
| Adapters | `actions/*/` | tool setup, env wiring | business decisions |
| Infrastructure | `actions/*/scripts/` | real logic, `set -euo pipefail`, locally runnable | `${{ github.* }}` references |

## Why this shape

| Principle | How it is applied |
|---|---|
| SRP | `ci.yml` only routes; each `reusable-*` tests one context; each action does one setup; each script function does one compose operation. |
| OCP | New check = new `reusable-*.yml` + one job block in `ci.yml`. Existing files are not edited. |
| LSP | Every reusable is substitutable through the same contract: typed `inputs`, boolean-ish `outputs`. |
| ISP | Callers receive only what they need (`db-password`, `node-version`); no god-object context. |
| DIP | Workflows depend on the `compose-run` / `setup-node-deps` abstractions; the script reads plain env (`DB_PASSWORD`), never `${{ github.* }}`. |
| DRY | Compose flags (`-f compose.yaml -f compose.ci.yaml`) and `checkout@v4` versions live in exactly one place each. |
| Clean architecture | Delivery (workflows) → use cases (reusables) → adapters (composite actions) → infrastructure (bash). Dependencies point inward: YAML never embeds shell logic. |

## Local verification (no push needed)

```bash
DB_PASSWORD='local-pw' bash .github/actions/compose-run/scripts/compose.sh config
DB_PASSWORD='ci-backend-password' bash .github/actions/compose-run/scripts/compose.sh backend-test
bash .github/actions/guard-framework/scripts/guard.sh   # framework conformance
```

## Enforcement (how the framework defends itself)

`reusable-guards` runs first and ungated on every CI run (`changes` needs it,
so a violation fails fast before docker jobs burn minutes). It enforces six rules:

| # | Rule | Catches |
|---|---|---|
| 1 | `ci.yml` contains no `run:` steps | fat orchestrator — logic leaking into the composition root |
| 2 | only `ci.yml` defines triggers; every `reusable-*` is `workflow_call` | bypassed fan-out — a partial triggered directly, skipping gates |
| 3 | actions use `${{ github.action_path }}`, never `${{ github.workspace }}/...` | unportable action coupled to repo layout |
| 4 | every `actions/**/*.sh` has `set -euo pipefail` | sloppy script failing silently |
| 5 | every job declares `timeout-minutes` | hung job burning minutes forever |
| 6 | no inline `docker compose` in workflows | logic bypassing the `compose-run` adapter |

To make violations actually block merge, mark `guards` as a **required status
check** (repo Settings → Branches → branch protection). Optional second layer:
a `CODEOWNERS` entry for `.github/**` so framework changes always get a human review.

## Adding a new check (example: lint)

1. Create `workflows/reusable-lint.yml` with `on: workflow_call` and one job.
2. If it needs tool setup used elsewhere, add `actions/<tool>/action.yml`; else keep steps local.
3. Put real logic in `actions/<tool>/scripts/*.sh` (private, co-located) so it stays
   locally runnable; extract to `scripts/common/` only when a second consumer appears.
4. Add one job block in `ci.yml` wired to `needs: changes` with its own `if:` gate.
5. Extend the `e2e` filter in `reusable-changes.yml` only if the new check can break the stack.

## Conventions

- Orchestrator (`ci.yml`): `uses:` only, plus `needs:`/`if:` gates. Least-privilege `permissions`, `concurrency.cancel-in-progress`, and `timeout-minutes` on every job.
- Reusables: `workflow_call` with typed `inputs` (with defaults) and documented `outputs`. Reference local actions via `./.github/actions/<name>`.
- Composites: small `action.yml` with `description`, typed `inputs`, `runs.using: composite`. No business decisions. Private scripts live in `scripts/` beside it, referenced via `${{ github.action_path }}` (never via `github.workspace` paths — that reintroduces coupling to repo layout).
- Scripts: `set -euo pipefail`, one subcommand per operation, `usage` on unknown input, `DB_PASSWORD` fail-fast mirroring `compose.yaml`.
