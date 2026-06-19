#!/usr/bin/env bash
# Run attribution_verify.sh on OP13 (USB) and OP12 (wireless root bench).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
export ADB="$(resolve_smoke_adb)"
OP13_SERIAL="${OP13_SERIAL:-8bf09993}"
OP12_SERIAL="${OP12_SERIAL:-192.168.1.2:44487}"

log() { echo "[attr_dual] $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

wait_for_device() {
  local serial="$1"
  local label="$2"
  local tries="${3:-15}"
  local i state
  for i in $(seq 1 "${tries}"); do
    state="$("${ADB}" -s "${serial}" get-state 2>/dev/null || true)"
    if [[ "${state}" == "device" ]]; then
      log "${label} online (${serial})"
      return 0
    fi
    log "Waiting for ${label} (${serial}) state=${state:-offline} (${i}/${tries})"
    "${ADB}" connect "${serial}" >/dev/null 2>&1 || true
    sleep 2
  done
  return 1
}

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
[[ -f app/build/outputs/apk/debug/app-debug.apk ]] || ./gradlew assembleDebug

log "=== OP13 (USB, adb root) ==="
wait_for_device "${OP13_SERIAL}" "OP13" 5 || fail "OP13 ${OP13_SERIAL} not online"
SMOKE_DEVICE="${OP13_SERIAL}" bash "${SCRIPT_DIR}/attribution_verify.sh"

log "=== OP12 (wireless, system root) ==="
"${ADB}" connect "${OP12_SERIAL}" >/dev/null 2>&1 || true
if wait_for_device "${OP12_SERIAL}" "OP12" 20; then
  SMOKE_DEVICE="${OP12_SERIAL}" FORCE_ROOT_SMOKE=1 bash "${SCRIPT_DIR}/attribution_verify.sh"
else
  fail "OP12 ${OP12_SERIAL} offline — enable wireless debugging on OP12 and re-run"
fi

log "PASS: attribution_dual_verify complete"
log "Record OP13: Smoke attr_verify: PASS $(date -u +%Y-%m-%dT%H:%M:%SZ) ${OP13_SERIAL}"
log "Record OP12: Smoke attr_verify: PASS $(date -u +%Y-%m-%dT%H:%M:%SZ) ${OP12_SERIAL} FORCE_ROOT_SMOKE=1"
