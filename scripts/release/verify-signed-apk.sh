#!/usr/bin/env bash
# Verify APK has a valid v2/v3 signature (apksigner).
set -euo pipefail

APK="${1:-}"
log() { echo "[verify-signed-apk] $*"; }
fail() { echo "[verify-signed-apk] FAIL: $*" >&2; exit 1; }

[[ -n "${APK}" ]] || fail "Usage: verify-signed-apk.sh <apk-path>"
[[ -f "${APK}" ]] || fail "APK not found: ${APK}"

find_apksigner() {
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    local candidate
    candidate="$(find "${ANDROID_HOME}/build-tools" -name apksigner -type f 2>/dev/null | sort -V | tail -1)"
    [[ -n "${candidate}" ]] && echo "${candidate}" && return 0
  fi
  local win_sdk="${LOCALAPPDATA}/Android/Sdk/build-tools"
  if [[ -d "${win_sdk}" ]]; then
    local candidate
    candidate="$(find "${win_sdk}" -name apksigner.bat -type f 2>/dev/null | sort -V | tail -1)"
    [[ -n "${candidate}" ]] && echo "${candidate}" && return 0
  fi
  command -v apksigner 2>/dev/null || true
}

APKSIGNER="$(find_apksigner)" || fail "apksigner not found — set ANDROID_HOME"

log "Verifying ${APK}"
"${APKSIGNER}" verify --verbose "${APK}" || fail "apksigner verify failed"

log "PASS: APK is signed"
