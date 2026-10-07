#!/usr/bin/env bash
# Co-located with the `compose-run` composite action (its only consumer).
# High cohesion: action + script version and move together; the action is
# portable across repos via github.action_path.
#
# Clean-architecture role: infrastructure detail. Workflows (delivery) never
# embed `docker compose` flags; they call this script through the
# `compose-run` composite action (adapter), passing configuration via env
# (dependency inversion). The script is runnable locally, so CI logic is
# testable outside GitHub Actions:
#
#   DB_PASSWORD='local-pw' bash .github/actions/compose-run/scripts/compose.sh config
#   DB_PASSWORD='local-pw' bash .github/actions/compose-run/scripts/compose.sh backend-test
#
# Operations:
#   config        validate the compose model (fast fail on bad YAML)
#   backend-test  build + run the containerized backend test suite
#   down-test     release backend-test resources (safe to run always)
#   e2e-up        build + run postgres/backend/frontend + Playwright suite
#   logs-e2e      dump the e2e stack logs for failure telemetry (safe on failure)
#   down-e2e      release the e2e stack (safe to run always)
set -euo pipefail

COMPOSE_ARGS=(-f compose.yaml -f compose.ci.yaml)

require_db_password() {
  if [[ -z "${DB_PASSWORD:-}" ]]; then
    echo "::error::DB_PASSWORD is not set (compose.yaml fail-fasts on \${DB_PASSWORD:?...})" >&2
    exit 1
  fi
}

cmd="${1:-help}"
case "$cmd" in
  config)
    # --quiet validates without printing interpolated values: a bare `config`
    # echoes resolved secrets (DB_PASSWORD) into the job log.
    docker compose "${COMPOSE_ARGS[@]}" config --quiet
    ;;
  backend-test)
    require_db_password
    docker compose "${COMPOSE_ARGS[@]}" config >/dev/null
    docker compose "${COMPOSE_ARGS[@]}" --profile test run --build --rm backend-test
    ;;
  down-test)
    docker compose "${COMPOSE_ARGS[@]}" --profile test down -v --remove-orphans
    ;;
  e2e-up)
    require_db_password
    docker compose "${COMPOSE_ARGS[@]}" --profile e2e up --build --abort-on-container-exit --exit-code-from e2e
    ;;
  logs-e2e)
    # Failure telemetry: containers are stopped (not removed) after e2e-up,
    # so their logs are still readable — dump them into the uploaded paths
    # BEFORE down-e2e destroys the evidence. Best-effort by design.
    mkdir -p e2e/ci-artifacts/compose-logs
    docker compose "${COMPOSE_ARGS[@]}" --profile e2e logs --no-color \
      > e2e/ci-artifacts/compose-logs/compose.log 2>&1 || true
    ;;
  down-e2e)
    docker compose "${COMPOSE_ARGS[@]}" --profile e2e down -v --remove-orphans
    ;;
  *)
    echo "usage: $0 {config|backend-test|down-test|e2e-up|logs-e2e|down-e2e}" >&2
    exit 2
    ;;
esac
