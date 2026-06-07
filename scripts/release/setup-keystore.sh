#!/usr/bin/env bash
# Create local release keystore + keystore.properties (gitignored).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[setup-keystore] $*"; }
fail() { echo "[setup-keystore] FAIL: $*" >&2; exit 1; }

KEYSTORE_DIR="${KEYSTORE_DIR:-${HOME}/.screen-wakelock-detector}"
KEYSTORE_PATH="${KEYSTORE_PATH:-${KEYSTORE_DIR}/release.jks}"
PROPS_FILE="${ROOT}/keystore.properties"
ALIAS="${RELEASE_KEY_ALIAS:-release}"
DNAME="${RELEASE_DNAME:-CN=Screen Wakelock Detector, OU=Release, O=Screen Wakelock Detector, C=US}"

find_java() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/keytool" ]]; then
    echo "${JAVA_HOME}/bin/keytool"
    return 0
  fi
  local win_jbr="/c/Program Files/Android/Android Studio/jbr/bin/keytool"
  if [[ -x "${win_jbr}" ]]; then
    echo "${win_jbr}"
    return 0
  fi
  command -v keytool
}

KEYTOOL="$(find_java)" || fail "keytool not found — set JAVA_HOME"

mkdir -p "${KEYSTORE_DIR}"

if [[ ! -f "${KEYSTORE_PATH}" ]]; then
  if [[ -z "${RELEASE_KEY_PASSWORD:-}" ]]; then
    fail "Set RELEASE_KEY_PASSWORD for non-interactive keystore creation"
  fi
  log "Creating keystore at ${KEYSTORE_PATH}"
  "${KEYTOOL}" -genkeypair -v \
    -keystore "${KEYSTORE_PATH}" \
    -alias "${ALIAS}" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "${RELEASE_KEY_PASSWORD}" \
    -keypass "${RELEASE_KEY_PASSWORD}" \
    -dname "${DNAME}"
else
  log "Keystore already exists: ${KEYSTORE_PATH}"
fi

if [[ ! -f "${PROPS_FILE}" ]]; then
  STORE_FILE="${KEYSTORE_PATH}"
  if command -v cygpath >/dev/null 2>&1; then
    STORE_FILE="$(cygpath -m "${KEYSTORE_PATH}")"
  fi
  STORE_FILE="${STORE_FILE//\\//}"
  cat > "${PROPS_FILE}" <<EOF
storeFile=${STORE_FILE}
storePassword=${RELEASE_KEY_PASSWORD:?RELEASE_KEY_PASSWORD required to write keystore.properties}
keyAlias=${ALIAS}
keyPassword=${RELEASE_KEY_PASSWORD}
EOF
  log "Wrote ${PROPS_FILE}"
else
  log "keystore.properties already exists — not overwriting"
fi

log "PASS: keystore ready"
