#!/usr/bin/env bash
# Run pinned actionlint over the repo (GitHub workflow schema validation).
# Always downloads the pinned binary so CI validates with exactly the version
# developers use locally — no "works on my machine" drift.
set -euo pipefail

ver="${ACTIONLINT_VERSION:?ACTIONLINT_VERSION is not set}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

curl -sSL --max-time 120 -o "$tmp/actionlint.tar.gz" \
  "https://github.com/rhysd/actionlint/releases/download/v${ver}/actionlint_${ver}_linux_amd64.tar.gz"
tar -xzf "$tmp/actionlint.tar.gz" -C "$tmp" actionlint
"$tmp/actionlint" -no-color
