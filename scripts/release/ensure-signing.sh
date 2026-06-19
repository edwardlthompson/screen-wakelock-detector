#!/usr/bin/env bash
# Ensure release signing is configured (keystore.properties or RELEASE_* env).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${ROOT}"

log() { echo "[ensure-signing] $*"; }
fail() { echo "[ensure-signing] FAIL: $*" >&2; exit 1; }

load_dotenv() {
  local env_file="${ROOT}/.env"
  [[ -f "${env_file}" ]] || return 0
  # shellcheck disable=SC1090
  set -a
  source "${env_file}"
  set +a
  log "Loaded ${env_file}"
}

load_dotenv

if [[ -f "${ROOT}/keystore.properties" ]]; then
  log "keystore.properties present"
  exit 0
fi

KEYSTORE_DEFAULT="${HOME}/.screen-wakelock-detector/release.jks"
if [[ -f "${KEYSTORE_DEFAULT}" ]]; then
  PW="${RELEASE_KEY_PASSWORD:-${RELEASE_STORE_PASSWORD:-}}"
  ALIAS="${RELEASE_KEY_ALIAS:-release}"
  if [[ -n "${PW}" ]]; then
    STORE_FILE="${KEYSTORE_DEFAULT}"
    if command -v cygpath >/dev/null 2>&1; then
      STORE_FILE="$(cygpath -m "${KEYSTORE_DEFAULT}")"
    fi
    STORE_FILE="${STORE_FILE//\\//}"
    cat > "${ROOT}/keystore.properties" <<EOF
storeFile=${STORE_FILE}
storePassword=${PW}
keyAlias=${ALIAS}
keyPassword=${PW}
EOF
    log "Wrote keystore.properties → ${KEYSTORE_DEFAULT}"
    exit 0
  fi
fi

if [[ -n "${RELEASE_STORE_FILE:-}" && -f "${RELEASE_STORE_FILE}" ]] \
  && [[ -n "${RELEASE_STORE_PASSWORD:-}" ]] \
  && [[ -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  log "Using RELEASE_* environment variables"
  exit 0
fi

fail "Release signing not configured. Set RELEASE_KEY_PASSWORD in .env (see .env.example) or create keystore.properties"
