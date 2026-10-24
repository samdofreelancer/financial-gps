# Agent Notes

Scratch space for agent-discovered knowledge that hasn't earned a permanent place yet.

**Read this when:** `AGENTS.md` answered your question but something felt off, or you hit
something the index doesn't cover.

**Do not read this when:** you just need a command. `AGENTS.md` has those.

## How this file works

`AGENTS.md` is curated and short on purpose — a line only lands there once it is true and
stays true. This file is the overflow: findings, dead ends, and environment quirks that are
real but not yet stable enough to promise to every future session.

Promotion rule:

```text
observed once   → stays here
observed twice  → fix or remove; if still true, promote to AGENTS.md
obsolete        → delete, don't annotate
```

A stale note here is worse than no note: the next agent has no way to tell it apart from a
fresh one. Delete when it's wrong.

## Gotcha capture template

Copy this block, fill every field. A gotcha is only useful to a future agent if it can be
**recognized from its symptom** — they will hit the error message, not the cause.

```markdown
### <short symptom, not cause>

- **Symptom:** <exact error text or observable behavior, verbatim>
- **Cause:** <one line — the thing that is actually true>
- **Fix:** <the command or action that resolves it>
- **Detection:** <how you'd know it's this and not an adjacent problem>
- **Seen:** <date>, <one line context: which command/task surfaced it>
```

Skip `Detection` only when the symptom is already unambiguous.

---

## Findings

### Log files are tracked in git

- **Symptom:** running the backend or the Vite dev server dirties `git status` with
  `frontend/dev.log` and/or `backend/backend.log` as modified.
- **Cause:** both files are tracked (`git ls-files backend/backend.log frontend/dev.log`)
  and neither appears in `.gitignore`.
- **Fix:** untrack and ignore them —
  `git rm --cached backend/backend.log frontend/dev.log`, then add both to `.gitignore`.
  Anything already committed stays in history; rewriting is a separate decision.
- **Detection:** they show up in `git status` right after a local dev-server run, with no
  source file touched.
- **Seen:** 2026-10-11, while auditing the repo for the first `AGENTS.md` write-up.

### Containerized backend tests need `host.docker.internal`

- **Symptom:** `docker compose --profile test run --rm backend-test` starts, but the
  Testcontainers PostgreSQL is unreachable and the suite fails on connect.
- **Cause:** Testcontainers starts Postgres as a sibling container, so it must reach the
  Docker host. `compose.override.yaml` handles this with
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` plus an `extra_hosts` entry mapping it
  to `host-gateway`.
- **Fix:** always run these through `compose.override.yaml` (plain `docker compose` already
  loads it). Do not hand-roll a `docker run` of `maven:3.9.9-eclipse-temurin-21` — you lose
  the host mapping.
- **Detection:** the failure only appears in the containerized runner; `./mvnw -B test`
  directly on the host works fine.
- **Seen:** 2026-10-11, reading `compose.override.yaml`.

### `compose.override.yaml` mounts sources — CI does not

- **Symptom:** local code changes appear to have no effect until a rebuild, or a change
  that passes locally behaves differently in CI.
- **Cause:** `compose.override.yaml` bind-mounts `./backend` and `./frontend` with
  hot-reload commands, while `compose.ci.yaml` builds images with no mounts. They are
  deliberately different runtimes.
- **Fix:** debug with `docker compose up --build` (override, hot reload). Reproduce CI with
  `docker compose -f compose.yaml -f compose.ci.yaml --profile e2e up --build
  --abort-on-container-exit --exit-code-from e2e`.
- **Detection:** a node_modules or compiled-assets difference that only shows in the CI path.
- **Seen:** 2026-10-11.

### `engine-contract.md` describes an engine the code does not have

- **Symptom:** you read `specs/financial-domain/contracts/engine-contract.md`, then look for the
  allocation/route/ETA fields it promises in `FinancialEngine`, and find a four-field result with no
  route, no allocation, and no destination at all.
- **Cause:** `domain/engine/FinancialEngine.calculate` returns only
  `(asOf, position, provenance, explanations)` — cash flow plus provenance. The richer contract is
  **aspirational**: the real GPS orchestration lives in
  `application/gps/usecase/CalculateFinancialGpsUseCase`, which bypasses `FinancialEngine` entirely.
  Only `ProfileAssembler` calls the engine.
- **Fix:** for GPS behaviour, read `CalculateFinancialGpsUseCase` + `domain/gps/*`. Treat
  `engine-contract.md` as a target shape, not as a description of current behaviour. Any feature that
  needs a route must decide whether to extend the application orchestration or finally promote
  `FinancialEngine` — do not assume the contract already exists.
- **Detection:** `FinancialEngine` has 3 callers, all in `ProfileAssembler`; nothing under
  `application/gps` references it.
- **Seen:** 2026-10-19, auditing 008 (timeline + allocation) against existing domain contracts.

### `Portfolio.goals()` is `List<Object>`, so the engine cannot type a goal

- **Symptom:** writing allocation or ETA logic against `Portfolio` produces `Object` where a `Goal`
  is expected, forcing a cast or an `instanceof` in the domain layer.
- **Cause:** `domain/model/Portfolio` predates Feature 003 owning goals; its Javadoc still says goals
  "stay untyped until 003 owns them". `Goal` exists in `domain/goal`, but `Portfolio` was never
  updated to reference it.
- **Fix:** before any feature needs to read goal fields through the portfolio, change the field to
  `List<Goal>` (and update the 3-4 construction sites). Until then, features that need typed goals
  must read them through an application port, not through `Portfolio`.
- **Detection:** `grep -n "List<Object> goals" backend/src/main/java/com/financialgps/domain/model/Portfolio.java`.
- **Seen:** 2026-10-19, while mapping which 008 inputs already exist in the domain.

### `FinancialPolicy` carries rounding modes only — no allocation or status policy

- **Symptom:** you look for the configurable `latenessTolerance`, route order, or allocation policy
  value and cannot find it next to the other "policy" defaults.
- **Cause:** `domain/policy/FinancialPolicy` is a two-field record
  `(displayRounding, countRounding)` — the subset 001 needed. Tolerance and status bands live in
  `domain/gps` (`GpsStatusPolicy`); there is no allocation policy object at all.
- **Fix:** do not extend `FinancialPolicy` by assumption. Policy values are currently per-feature
  domain constants/objects; a route policy would need its own type. Check `domain/gps/` and
  `status-rules.md` before claiming a value is configurable.
- **Detection:** `FinancialPolicy` has exactly two components; anything else called "policy" in the
  specs is not backed by it.
- **Seen:** 2026-10-19, when 008 needed a documented, disclosed route-order policy.

### GitHub MCP server fails with "Authorization header is badly formatted"

- **Symptom:** `/mcps` shows `github` as `failed` with
  `bad request: Authorization header is badly formatted (HTTP 400)`.
- **Cause:** `opencode.jsonc` sent `Authorization: Bearer {env:GITHUB_PERSONAL_ACCESS_TOKEN}`,
  but an unset `{env:...}` becomes an **empty string** in OpenCode, so the request carried
  `Bearer ` with no token. The variable existing at *user scope* does not help: the background
  OpenCode service captured its environment when it started, and any process it spawns (including
  shell tools) inherits that stale block.
- **Fix:** the header now reads `Bearer {file:~/.secrets/github-pat}`. After rotating the PAT,
  re-snapshot the registry value with `~/.config/opencode/scripts/sync-github-pat.sh`
  (writes 40 bytes, no trailing newline — a newline corrupts the header the same way).
- **Detection:** an empty bearer reproduces the exact `400`, but `400` alone does **not** prove
  emptiness: a well-formed token that is not Copilot-entitled also returns `400` from
  `api.githubcopilot.com/models` (verified with the github.com PAT, which returns `200` for
  github.com and `400` here). Check the file's byte count before blaming the substitution; a
  wrong-but-entitled token gives `401 token authentication failed`.
- **Seen:** 2026-10-22, `opencode mcp list` showing `github` failed.

### Permission rules seem ignored — nothing ever prompts

- **Symptom:** `permissions` rules with `"effect": "ask"` are added to `opencode.jsonc`, yet
  every tool call runs silently: edits, `git push`, MCP writes.
- **Cause:** client CLI setting `session.permissions` is `autoaccept` (the "Auto-accept
  permissions" toggle), which answers every `ask` on the client side. It never reads the rule
  file, so editing rules cannot help. `deny` is still enforced — only `ask` is swallowed.
- **Fix:** put `{ "session": { "permissions": "prompt" } }` in `~/.config/opencode/cli.json`,
  restart the client, confirm the toggle reads off. Project config cannot override it.
- **Detection:** an `ask` never prompts but a `deny` for the same resource still blocks — that
  proves the file loads and the client is auto-answering.
- **Seen:** 2026-10-22, permission rules in `opencode.jsonc` had no visible effect.

### A `permissions` rule you just added loses to an earlier `allow`

- **Symptom:** `edit opencode.jsonc` is still allowed after adding an `ask` for it; more
  surprisingly `edit .env.example` comes back **denied** despite an explicit `allow`.
- **Cause:** rules merge into one ordered list and the **last** match wins, so an `ask`/`deny`
  only narrows rules **above** it. V2's base policy already opens with
  `{ "*", "*", "allow" }`, and `*.env.example` matches a later `*.env.*` pattern.
- **Fix:** order the array blanket `ask` → specific `allow` → re-tightened `ask` → `deny` last,
  and place an exception (`*.env.example`) *after* the pattern it overlaps (`*.env.*`).
- **Detection:** for the resource, scan every rule below yours; if any matches, that one wins.
- **Seen:** 2026-10-22, caught by simulating wildcard matching over ~24 rule/path cases.

### `.env` readable with no prompt once reads are allowed

- **Symptom:** the base policy's prompt for `.env` disappears after adding a broad read allow.
- **Cause:** `read * → allow` is appended after the base policy's `read *.env → ask`, and
  last-match-wins. Same trap applies to `edit`.
- **Fix:** re-add `*.env` and `*.env.*` → `ask` plus `*.env.example` → `allow` **after** the
  blanket read allow. `.env` holds `DB_PASSWORD`.
- **Detection:** `read .env` returns content without prompting.
- **Seen:** 2026-10-22.

### `edit *` also auto-allows writes outside the project

- **Symptom:** editing `C:/Users/.../Temp/...` or another repo proceeds with no prompt.
- **Cause:** `*` matches `/` and every other character, so it matches canonical absolute
  external paths too. Internal paths reach a rule in Location-relative form; absolute paths are
  external by definition.
- **Fix:** after `edit * → allow`, add `?:/*` and `/*` → `ask` (drive-letter and POSIX absolute
  forms). Order matters as above.
- **Detection:** test the rule with an absolute external path — a repo-relative path cannot
  reveal it.
- **Seen:** 2026-10-22.

### `grep` with an explicit path reads gitignored files

- **Symptom:** `grep` never returns `.env`, but passing a path — even a directory — does
  (`grep PROBE path=backend/target` found a gitignored file).
- **Cause:** the default search honours `.gitignore`, so `.env` is invisible; supplying `path`
  disables that filtering. The `grep` permission action's resource is the **regular expression**,
  not the path, so no rule can scope it — a `deny` on `grep` only sees text that looks like a
  regex.
- **Fix:** nothing in `permissions` closes this. Either keep secrets out of the working tree
  (`docker compose --env-file ~/.secrets/...`), or accept that `grep path=.env` bypasses
  `read *.env → ask`. `cat`/`head`/`read` are still gated.
- **Detection:** a probe file under a gitignored directory (`backend/target/`) found by grep only
  when `path` is supplied.
- **Seen:** 2026-10-22, verifying whether the `.env` prompt could be bypassed.

### A filename-shaped pattern does not match an absolute path

- **Symptom:** `{ "resource": "id_rsa*", "effect": "deny" }` denies `read id_rsa` but still allows
  `read C:/Users/<user>/.ssh/id_rsa`.
- **Cause:** the resource is the whole path; `id_rsa*` only matches strings that *start* with it.
  External paths arrive canonical and absolute, internal ones Location-relative.
- **Fix:** add the `*/`-prefixed twin (`*/id_rsa*`). Same reason `*.pem` works everywhere — its
  leading `*` already spans the drive prefix.
- **Detection:** simulate both a relative and an absolute path against the rule.
- **Seen:** 2026-10-22, first version of the key-material deny rules.

### Inverting shell to `allow *` re-opens secret-printing "read" commands

- **Symptom:** after replacing the read-only allowlist with `{ "shell", "*", "allow" }`,
  `docker inspect <container>` and `docker compose config` run silently — the first prints
  `Config.Env` (POSTGRES_PASSWORD), the second prints the *resolved* `DB_PASSWORD`.
- **Cause:** the narrow allowlist used to leave every unlisted command at `ask`; a blanket
  allow flips that. Both commands read as "inspect", so a secret-pattern rule (`* *.env*`,
  `* *.secrets*`) does not catch them — the secret is already resolved in memory/container env.
- **Fix:** keep an explicit `ask` for `docker inspect*`, `docker compose config*`,
  `docker compose * config*`, `docker compose exec*` and `docker build *` inside the inverted
  block, and let the `down -v` / `volume rm` denies stay last.
- **Detection:** list every command that can print an env var or a resolved config, then
  simulate it — a "read-only" name is not evidence.
- **Seen:** 2026-10-22, right after the inversion.

### Moving the secret out of the tree makes every documented compose command prompt

- **Symptom:** `* *.secrets* → ask` (added to stop `cat` printing the password) also fires on
  `docker compose --env-file ~/.secrets/financial-gps.env up --build`, i.e. on the exact command
  `AGENTS.md` now tells everyone to run.
- **Cause:** the permission check sees the whole command line, and the env-file path is part of
  it. A path-based rule cannot tell "pass this file to compose" from "print this file".
- **Fix:** allow the safe verbs explicitly, after the `* *.secrets*` ask:
  `docker compose --env-file ~/.secrets/* up*`, `… ps*`, `… logs*`, and the
  `… --profile * up*` / `… --profile * run*` forms. Keep the list verb-scoped — `config`,
  `exec` and `down` must stay out, or the allow re-opens the leak above.
- **Detection:** simulate the documented commands verbatim, not an abbreviated form.
- **Seen:** 2026-10-22, first simulator run after the inversion (2 of 76 cases failed).

### Container backend tests fail on `client version 1.32 is too old`

- **Symptom:** every Spring integration test errors at startup with
  `BadRequestException` / `Status 400: {"message":"client version 1.32 is too old..."}` from
  docker-java, while the same command worked before the Docker engine upgrade.
- **Cause:** Testcontainers' bundled docker-java negotiates Docker API `1.32`; Docker Engine 29
  (WSL2 here) refuses anything below `1.40`.
- **Fix:** add `-Dapi.version=1.56` to the test JVM:
  `… backend-test ./mvnw -B test -Dtest=ClassName -DargLine="-Dapi.version=1.56"`.
  No repo change is needed; docker-java's `DefaultDockerClientConfig` reads the property.
- **Detection:** the message names the client API version, not the container or the DB.
- **Seen:** 2026-10-24, first containerized run of the 005 API tests.

### Mockito cannot attach the Byte Buddy agent inside the test container

- **Symptom:** `java.lang.IllegalStateException: Could not initialize plugin: interface
  org.mockito.plugins.MockMaker`, root cause
  `com.sun.tools.attach.AttachNotSupportedException: Unable to open socket file
  /proc/<pid>/root/tmp/.java_pid<pid>: target process <pid> doesn't respond within 10500ms`.
- **Cause:** the inline mock maker self-attaches through the JDK attach listener; that listener
  does not come up on demand in this container/kernel combination, so the socket never answers.
- **Fix:** start the listener at JVM startup: add `-XX:+StartAttachListener` to the surefire
  `argLine` for the local run. `-Djdk.attach.allowAttachSelf=true` alone does **not** help.
- **Detection:** the failing stack passes through `InlineDelegateByteBuddyMockMaker.<init>` and
  `ByteBuddyAgent.install`, even for a test class with no `@MockBean`.
- **Seen:** 2026-10-24, `GoalControllerTest#unauthenticatedGoalReadIs401` in `backend-test`.

### An `asOf` before today shows Available Capacity `0.00`

- **Symptom:** a seeded income of 40M is ignored and a goal stage gets
  `etaReasonCode = NO_AVAILABLE_CAPACITY` (or `requiredMonthlyCapacity` equals the whole
  remaining amount) when the request passes an `asOf` in the past.
- **Cause:** 001 income/expense rows are effective-dated; the adapter only counts lines whose
  `effectiveFrom` is on or before `asOf`. A request evaluated at an earlier date legitimately
  has no effective income yet.
- **Fix:** write assertions against `LocalDate.now(UTC).plusDays(1)` (the nearest deterministic
  date on which today's seeded facts are effective), not a fixed calendar date.
- **Detection:** the same facts produce capacity when `asOf` is omitted.
- **Seen:** 2026-10-24, live probe of `GET /api/v1/roadmap?asOf=2026-10-22`.

### A checked `tasks.md` box does not mean implemented

- **Symptom:** `specs/005-financial-roadmap/tasks.md` carried `[x]` on T004 while the rest of the
  slice was still uncommitted, so a fresh session could read the boxes as delivery state and skip
  verification.
- **Cause:** a box is ticked when the task's artefact exists locally, not when the change is
  merged; the file says nothing about `git status`.
- **Fix:** verify against the working tree (the file the task names, plus `git status`) before
  trusting a checkbox: a box means the artefact exists and its verification passed, not that the
  change is merged. The delivery pass (T047) ticks a box only after the task's test or check ran
  green.
- **Detection:** `git status` shows the named files as untracked or modified.
- **Seen:** 2026-10-24, resuming Feature 005.

### Roadmap stage order is not the §8 allocation order

- **Symptom:** the temptation to reuse the `calculation-rules.md` §8 waterfall (committed debt
  extra → emergency fund → goals by priority → remaining) as the stage order, which would make
  stage position imply funding.
- **Cause:** §8 orders **money allocation** (Feature 008); Feature 005 only presents an ordered
  checklist of stages.
- **Fix:** `RoadmapGenerator` uses `debt elimination → emergency-fund goals → remaining goals`,
  publishes `orderPolicy.scope = ROADMAP_STAGE_ORDER`, and the oracle §8 waterfall stays
  unchanged (only §8.1 was added to document the distinction).
- **Detection:** any 005 code that emits `allocationStatus`, a funded/shortfall amount or
  `ROUTE_PROJECTION`/`ROUTE_ALLOCATION` is wrong by construction.
- **Seen:** 2026-10-24, implementing T015/T034.

### `specs/005`'s original draft names React paths

- **Symptom:** the 2026-08-24 draft artefacts under `specs/005-financial-roadmap/` mention
  `src/pages/*.tsx` / `src/components/*.tsx`.
- **Cause:** the draft predates the Vue 3 + Pinia frontend.
- **Fix:** translate to the real layout: `frontend/src/views/RoadmapView.vue`,
  `frontend/src/components/roadmap/*.vue`, `frontend/src/stores/roadmapStore.ts`,
  `frontend/src/api/roadmap.ts`.
- **Detection:** any `.tsx` path in a 005 task.
- **Seen:** 2026-10-24, during the T027/T036 frontend pass.

## Dead ends

Things tried that did not work — recorded so nobody spends the time twice.

### Running `./mvnw` on the host to run backend tests

- **Symptom:** an agent reached for `cd backend && ./mvnw -B test`, then invented
  `export JAVA_HOME="/c/Program Files/Java/jdk-21"` and
  `export DB_PASSWORD="$(grep -E '^DB_PASSWORD=' ../.env | cut -d= -f2-)"` to make it work.
- **Cause:** the first `AGENTS.md` listed the host command and the container command as two
  equal rows with no default. `docker compose` substitutes `.env` automatically; nothing
  else in the repo does, so the host path looks broken until you hand-feed it the secret.
- **Fix:** `docker compose --env-file ~/.secrets/financial-gps.env --profile test run --rm backend-test`.
  No `JAVA_HOME`, no export, no grep — `backend-test` never reads `DB_PASSWORD` at all, since
  the suite uses its own Testcontainers Postgres. (The `--env-file` arrived 2026-10-22, when the
  real password left the working tree; compose still interpolates the whole file on any
  subcommand, so it is needed even here.)
- **Detection:** any `JAVA_HOME` export, or any `grep` of `.env` in a command. Neither should
  ever appear in this repo.
- **Seen:** 2026-10-11, immediately after writing the first version of `AGENTS.md`.

### Reading merged config from the running server with `opencode api`

- **Symptom:** `opencode api get /api/info` prints
  `Error: Expected an operation name or an HTTP method and path` and exits 1 — same for every
  path, uppercase or lowercase method, with and without `--`.
- **Cause:** argument parsing in `opencode` v2.1.3 (Windows) rejects the documented
  `METHOD /path` form before any request is made.
- **Fix:** don't rely on it. Verify effective permissions by reading `opencode.jsonc` plus
  `~/.config/opencode/cli.json` and evaluating the ordered rules yourself.
- **Detection:** `opencode api --help` works, so the subcommand exists — only invocation fails.
- **Seen:** 2026-10-22, trying to confirm the merged permission config.

## Open questions

Unresolved. If you answer one, move the answer to `AGENTS.md` or a doc under `docs/` and
delete it from here.

<!-- Add under this heading. Delete the section once it has entries. -->
