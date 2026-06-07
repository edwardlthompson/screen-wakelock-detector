#!/usr/bin/env bash
# Verify release APK artifacts: presence, mapping file, size ceiling, optional zipalign
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MAX_APK_BYTES=$((25 * 1024 * 1024))
RELEASE_DIR="${ROOT}/app/build/outputs/apk/release"
DEBUG_DIR="${ROOT}/app/build/outputs/apk/debug"
MAPPING_FILE="${ROOT}/app/build/outputs/mapping/release/mapping.txt"

log() { echo "[verify-release-apk] $*"; }
fail() { echo "[verify-release-apk] FAIL: $*" >&2; exit 1; }

find_release_apk() {
  local candidate
  for candidate in \
    "${RELEASE_DIR}/app-release.apk" \
    "${RELEASE_DIR}/app-release-unsigned.apk"
  do
    if [[ -f "${candidate}" ]]; then
      echo "${candidate}"
      return 0
    fi
  done
  return 1
}

has_debug_only() {
  local debug_apk
  for debug_apk in "${DEBUG_DIR}/app-debug.apk" "${DEBUG_DIR}/"*.apk; do
    if [[ -f "${debug_apk}" ]]; then
      return 0
    fi
  done
  return 1
}

APK=""
if APK="$(find_release_apk)"; then
  log "Found release APK: ${APK}"
else
  if has_debug_only; then
    fail "Only debug APK found — release build required (run assembleRelease)"
  fi
  fail "Release APK not found under ${RELEASE_DIR}"
fi

[[ -f "${MAPPING_FILE}" ]] || fail "ProGuard mapping missing: ${MAPPING_FILE} (isMinifyEnabled must be true)"

APK_SIZE=$(stat -c%s "${APK}" 2>/dev/null || stat -f%z "${APK}")
log "APK size: ${APK_SIZE} bytes (max ${MAX_APK_BYTES})"
if (( APK_SIZE > MAX_APK_BYTES )); then
  fail "APK exceeds 25 MB ceiling (${APK_SIZE} bytes)"
fi

if command -v zipalign >/dev/null 2>&1; then
  log "Running zipalign -c 4"
  zipalign -c 4 "${APK}" || fail "zipalign verification failed"
else
  log "zipalign not available — skipping alignment check"
fi

if [[ "${EXPECT_SIGNED:-0}" == "1" ]]; then
  VERIFY_SIGNED="${SCRIPT_DIR}/verify-signed-apk.sh"
  [[ -x "${VERIFY_SIGNED}" ]] || chmod +x "${VERIFY_SIGNED}"
  bash "${VERIFY_SIGNED}" "${APK}" || fail "Signed APK required (EXPECT_SIGNED=1)"
fi

log "PASS: release APK verified"
