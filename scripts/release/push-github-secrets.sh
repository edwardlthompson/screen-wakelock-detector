#!/usr/bin/env bash
# Push release signing secrets to GitHub Actions from local keystore.properties.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[push-github-secrets] $*"; }
fail() { echo "[push-github-secrets] FAIL: $*" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI not found"
gh auth status >/dev/null 2>&1 || fail "gh not authenticated — run: gh auth login"

PROPS="${ROOT}/keystore.properties"
[[ -f "${PROPS}" ]] || fail "keystore.properties missing — run setup-keystore.sh first"

storeFile="$(grep '^storeFile=' "${PROPS}" | cut -d= -f2- | tr -d '\r')"
storePassword="$(grep '^storePassword=' "${PROPS}" | cut -d= -f2- | tr -d '\r')"
keyAlias="$(grep '^keyAlias=' "${PROPS}" | cut -d= -f2- | tr -d '\r')"
keyPassword="$(grep '^keyPassword=' "${PROPS}" | cut -d= -f2- | tr -d '\r')"

[[ -f "${storeFile}" ]] || fail "Keystore not found: ${storeFile}"

if base64 --help 2>&1 | grep -q '\-w'; then
  B64="$(base64 -w0 "${storeFile}")"
else
  B64="$(base64 "${storeFile}" | tr -d '\n')"
fi

log "Setting GitHub secrets (keystore base64 + passwords)"
gh secret set RELEASE_STORE_FILE_B64 --body "${B64}"
gh secret set RELEASE_STORE_PASSWORD --body "${storePassword}"
gh secret set RELEASE_KEY_ALIAS --body "${keyAlias}"
gh secret set RELEASE_KEY_PASSWORD --body "${keyPassword}"

log "PASS: GitHub secrets updated"
