#!/usr/bin/env bash
# Run pinned actionlint over the repo (GitHub workflow schema validation).
# Always downloads the pinned binary so CI validates with exactly the version
# developers use locally — no "works on my machine" drift.
set -euo pipefail

ver="${ACTIONLINT_VERSION:?ACTIONLINT_VERSION is not set}"
# Pinned checksum for the linux/amd64 tarball (from the release checksums.txt).
# Without this a compromised release asset executes silently in CI.
sha256="023070a287cd8cccd71515fedc843f1985bf96c436b7effaecce67290e7e0757"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

curl -sSL --retry 3 --retry-all-errors --max-time 120 -o "$tmp/actionlint.tar.gz" \
  "https://github.com/rhysd/actionlint/releases/download/v${ver}/actionlint_${ver}_linux_amd64.tar.gz"
echo "${sha256}  $tmp/actionlint.tar.gz" | sha256sum -c -
tar -xzf "$tmp/actionlint.tar.gz" -C "$tmp" actionlint
"$tmp/actionlint" -no-color
