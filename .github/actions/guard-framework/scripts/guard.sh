#!/usr/bin/env bash
# Framework conformance guard. Enforces the rules documented in .github/README.md
# so a PR that bypasses the framework (fat orchestrator, directly-triggered
# partial, unportable action, sloppy script) fails fast instead of merging.
#
# Zero dependencies (grep/awk only). Runnable locally:
#
#   bash .github/actions/guard-framework/scripts/guard.sh
#
# Usage: guard.sh [repo-root]  (defaults to $GITHUB_WORKSPACE, else pwd)
set -euo pipefail

ROOT="${1:-${GITHUB_WORKSPACE:-$(pwd)}}"
cd "$ROOT"

FAIL=0
fail() { echo "::error::$1" >&2; FAIL=1; }

# Rule 1: the orchestrator routes only — no `run:` steps (comments ignored).
if sed 's/#.*//' .github/workflows/ci.yml | grep -nE '(^|[[:space:]-])run:' >/dev/null; then
  fail "ci.yml must not contain run: steps (orchestrator routes via uses: only)"
fi

# Rule 2: only ci.yml may define triggers; every reusable-* must be workflow_call.
for file in .github/workflows/reusable-*.yml; do
  if ! grep -q 'workflow_call' "$file"; then
    fail "$file: missing workflow_call trigger (partials must be callable-only)"
  fi
  if grep -qE '^[[:space:]]*(push|pull_request):' "$file"; then
    fail "$file: must not define push/pull_request triggers (only ci.yml is an entry point)"
  fi
done

# Rule 3: actions stay portable — reference own files via github.action_path,
# never by joining github.workspace into a repo-layout path. (Passing the
# workspace root itself as a script argument is fine; joining a path onto
# it reintroduces coupling to repo layout.)
if grep -rnE 'github\.workspace *\}\}/' .github/actions/*/action.yml; then
  fail "actions must use \${{ github.action_path }}, not \${{ github.workspace }}/... (portability)"
fi

# Rule 4: every private script runs in strict mode.
while IFS= read -r script; do
  if ! grep -q 'set -euo pipefail' "$script"; then
    fail "$script: missing 'set -euo pipefail' (strict mode required)"
  fi
done < <(find .github/actions -name '*.sh')

# Rule 5: every job declares timeout-minutes (a hung job must not burn minutes forever).
for file in .github/workflows/*.yml; do
  # List job headers, then verify each block has a timeout.
  while IFS= read -r job; do
    block=$(awk -v header="  $job" '
      $0 == header { in_block=1; next }
      in_block && /^  [A-Za-z0-9_.-]+:/ { exit }
      in_block { print }
    ' "$file")
    if ! grep -q 'timeout-minutes:' <<<"$block"; then
      fail "$file: job '${job%:}' has no timeout-minutes"
    fi
  done < <(awk '/^jobs:/{in_jobs=1;next} /^[^[:space:]#]/{in_jobs=0} in_jobs && /^  [A-Za-z0-9_.-]+:/{print $1}' "$file")
done

# Rule 6: real compose logic lives in scripts — no inline `docker compose` in workflows.
if grep -rn 'docker compose' .github/workflows/; then
  fail "workflows must not inline 'docker compose' (use the compose-run action)"
fi

if [[ "$FAIL" -eq 0 ]]; then
  echo "framework guards passed"
fi
exit "$FAIL"
