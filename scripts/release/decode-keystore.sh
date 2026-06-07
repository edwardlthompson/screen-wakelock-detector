#!/usr/bin/env bash
# Decode base64 release keystore for CI (GitHub Actions / GitLab).
# Sets RELEASE_STORE_FILE and related env when RELEASE_STORE_FILE_B64 is present.
set -euo pipefail

log() { echo "[decode-keystore] $*"; }

if [[ -z "${RELEASE_STORE_FILE_B64:-}" ]]; then
  if [[ -n "${RELEASE_STORE_FILE:-}" && -f "${RELEASE_STORE_FILE}" ]]; then
    log "Using existing RELEASE_STORE_FILE=${RELEASE_STORE_FILE}"
    exit 0
  fi
  log "RELEASE_STORE_FILE_B64 not set — unsigned release build"
  exit 0
fi

DEST="${RELEASE_STORE_FILE:-/tmp/release.jks}"
echo "${RELEASE_STORE_FILE_B64}" | base64 -d > "${DEST}"
chmod 600 "${DEST}"
export RELEASE_STORE_FILE="${DEST}"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "RELEASE_STORE_FILE=${DEST}"
    echo "RELEASE_STORE_PASSWORD=${RELEASE_STORE_PASSWORD:-}"
    echo "RELEASE_KEY_ALIAS=${RELEASE_KEY_ALIAS:-release}"
    echo "RELEASE_KEY_PASSWORD=${RELEASE_KEY_PASSWORD:-}"
    echo "EXPECT_SIGNED=1"
  } >> "${GITHUB_ENV}"
fi

log "Keystore decoded to ${DEST}"
