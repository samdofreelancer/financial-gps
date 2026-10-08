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

# Rule 2: reusable partials are callable-only — `workflow_call` must be the SOLE
# trigger. Any other trigger (push, schedule, even workflow_dispatch) makes the
# file directly runnable from the Actions UI, bypassing gates entirely.
for file in .github/workflows/reusable-*.yml; do
  triggers=$(awk '
    /^on:[[:space:]]*$/ { in_on=1; next }
    /^[^[:space:]#]/ { in_on=0 }
    in_on && /^  [A-Za-z_]+:/ {
      t=$1; sub(/:$/, "", t); print t
    }' "$file" | sort -u | tr '\n' ' ')
  if [[ "$triggers" != "workflow_call " ]]; then
    fail "$file: only 'workflow_call' is allowed as a trigger (found: ${triggers:-none}) — extra triggers are gate-bypassing entry points"
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

# Rule 5: timeouts live INSIDE the reusables — every job there declares
# timeout-minutes (a hung job must not burn minutes forever), while the
# orchestrator must NOT declare any (GitHub's schema forbids timeout-minutes
# alongside uses: and rejects the whole file — this exact bug shipped once).
for file in .github/workflows/reusable-*.yml; do
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
if grep -n 'timeout-minutes:' .github/workflows/ci.yml; then
  fail "ci.yml must not declare timeout-minutes (forbidden on reusable-caller jobs; set it inside the reusable)"
fi

# Rule 6: real compose logic lives in scripts — no inline `docker compose` in workflows.
if grep -rn 'docker compose' .github/workflows/; then
  fail "workflows must not inline 'docker compose' (use the compose-run action)"
fi

# Rule 7: third-party actions are SHA-pinned (tags are mutable — a moved tag
# silently changes what CI executes). Local actions (./...) need no pin.
while IFS= read -r ref; do
  case "$ref" in ./*|\$/*) continue ;; esac
  if ! [[ "$ref" =~ @[0-9a-f]{40}$ ]]; then
    fail "unpinned third-party action: '${ref}' (pin to a full commit SHA, keep the version as a '# vN' comment)"
  fi
done < <(grep -rhE '^[[:space:]]*-?[[:space:]]*uses:' .github/workflows/*.yml .github/actions/*/action.yml \
  | sed -E 's/.*uses:[[:space:]]*//; s/[[:space:]]*#.*//; s/["'\'']//g')

# Rule 8: the changes detector checks out full history. Dorny self-fetches what
# it needs, so this is robustness rather than strict necessity: no reliance on
# extra fetch roundtrips, no shallow-clone edge cases on push events (PRs use
# the API and are unaffected either way).
if ! grep -A5 'actions/checkout' .github/workflows/reusable-changes.yml | grep -q 'fetch-depth: 0'; then
  fail "reusable-changes.yml checkout must set fetch-depth: 0 (full history keeps push-event diffing robust)"
fi

# Rule 9: reusable jobs may only request permissions the caller grants —
# GitHub rejects the whole run when a nested job exceeds the caller's ceiling
# (permissions can only stay equal or shrink down the call chain). Unlisted
# permissions default to none once a permissions block exists.
perm_rank() { case "$1" in none) echo 0;; read) echo 1;; write) echo 2;; *) echo 0;; esac; }
declare -A CEIL=()
while IFS='=' read -r key val; do
  CEIL["$key"]="$val"
done < <(awk '
  /^permissions:[[:space:]]*$/ { in_top=1; next }
  /^[^[:space:]#]/ { in_top=0 }
  in_top && /^  [a-z-]+:[[:space:]]*(read|write|none)/ {
    line=$0; sub(/^  /, "", line); sub(/[[:space:]]*#.*$/, "", line)
    split(line, kv, /:[[:space:]]*/); print kv[1]"="kv[2]
  }' .github/workflows/ci.yml)
for file in .github/workflows/reusable-*.yml; do
  while IFS='=' read -r key val; do
    ceiling="${CEIL[$key]:-none}"
    if [[ "$(perm_rank "$val")" -gt "$(perm_rank "$ceiling")" ]]; then
      fail "$file requests '${key}: ${val}' but the ci.yml ceiling allows '${key}: ${ceiling}'"
    fi
  done < <(awk '
    /^    permissions:[[:space:]]*$/ { in_job=1; next }
    /^  [A-Za-z0-9_.-]+:/ { in_job=0 }
    /^[^[:space:]#]/ { in_job=0 }
    in_job && /^      [a-z-]+:[[:space:]]*(read|write|none)/ {
      line=$0; sub(/^      /, "", line); sub(/[[:space:]]*#.*$/, "", line)
      split(line, kv, /:[[:space:]]*/); print kv[1]"="kv[2]
    }' "$file")
done

# Rule 10: ci.yml is the sole entry point — every workflow file is either the
# orchestrator or a reusable partial. A stray file with its own triggers
# would bypass guards and path gates entirely.
for file in .github/workflows/*.yml; do
  case "$(basename "$file")" in ci.yml|reusable-*.yml) continue ;; esac
  fail "$file: only ci.yml and reusable-*.yml may exist here (stray workflow files bypass the framework)"
done

if [[ "$FAIL" -eq 0 ]]; then
  echo "framework guards passed"
fi
exit "$FAIL"
