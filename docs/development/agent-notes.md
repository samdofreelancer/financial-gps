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

## Dead ends

Things tried that did not work — recorded so nobody spends the time twice.

### Running `./mvnw` on the host to run backend tests

- **Symptom:** an agent reached for `cd backend && ./mvnw -B test`, then invented
  `export JAVA_HOME="/c/Program Files/Java/jdk-21"` and
  `export DB_PASSWORD="$(grep -E '^DB_PASSWORD=' ../.env | cut -d= -f2-)"` to make it work.
- **Cause:** the first `AGENTS.md` listed the host command and the container command as two
  equal rows with no default. `docker compose` substitutes `.env` automatically; nothing
  else in the repo does, so the host path looks broken until you hand-feed it the secret.
- **Fix:** `docker compose --profile test run --rm backend-test`. No `JAVA_HOME`, no export,
  no grep — `backend-test` never reads `DB_PASSWORD` at all, since the suite uses its own
  Testcontainers Postgres.
- **Detection:** any `JAVA_HOME` export, or any `grep` of `.env` in a command. Neither should
  ever appear in this repo.
- **Seen:** 2026-10-11, immediately after writing the first version of `AGENTS.md`.

## Open questions

Unresolved. If you answer one, move the answer to `AGENTS.md` or a doc under `docs/` and
delete it from here.

<!-- Add under this heading. Delete the section once it has entries. -->
