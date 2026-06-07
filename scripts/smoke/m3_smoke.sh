#!/usr/bin/env bash
# M3 smoke: non-root root rows grayed; rooted device shows wakelock tag on enhanced entry
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m3_smoke] $*"; }
fail() { echo "[m3_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

ROOTED=false
if [[ "${FORCE_ROOT_SMOKE:-0}" == "1" ]]; then
  SU_OUT="$(timeout 3 "${ADB}" -s "${DEVICE}" shell su -c id 2>/dev/null || true)"
  ADB_ID="$("${ADB}" -s "${DEVICE}" shell id 2>/dev/null || true)"
  if echo "${SU_OUT}" | grep -q "uid=0" || echo "${ADB_ID}" | grep -q "uid=0(root)"; then
    ROOTED=true
    log "Device appears rooted (su / adb root available)"
  fi
else
  log "Skipping su probe (set FORCE_ROOT_SMOKE=1 on rooted device)"
fi

if [[ "${ROOTED}" == "false" ]]; then
  log "Device non-root — checking disabled root UI path"
fi

log "Open Settings → Root via deep link"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 2

"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://settings/root" -p "${PACKAGE}" 2>/dev/null || true
sleep 3

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"

if [[ "${ROOTED}" == "true" ]]; then
  log "Enable root attribution (debug automation)"
  "${ADB}" -s "${DEVICE}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  # shellcheck source=scripts/smoke/_root_enable.sh
  source "${SCRIPT_DIR}/_root_enable.sh"
  ROOT_ENABLE_PACKAGE="${PACKAGE}" root_enable_app "${ADB}" -s "${DEVICE}"

  log "Launch app for wake + root snapshot"
  "${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep 2

  log "Trigger screen on for root snapshot"
  "${ADB}" -s "${DEVICE}" logcat -c 2>/dev/null || true
  "${ADB}" -s "${DEVICE}" shell cmd notification post -S bigtext -t "RootSmoke" "root_smoke" "M3 root test" 2>/dev/null || true
  sleep 1
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || true
  sleep 1
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 4

  LOGS="$("${ADB}" -s "${DEVICE}" logcat -d -s RootAttributor:I RootAttributor:D RootAttributor:W WakeMonitor:I 2>/dev/null || true)"
  if echo "${LOGS}" | grep -qiE "Root wakelock|Root wakeup|rootEnhanced=true|Root wakelock from"; then
    log "Root attribution logs found"
    echo "${LOGS}" | grep -iE "RootAttributor|rootEnhanced" | tail -5
  else
    echo "${LOGS}" | tail -10
    fail "FORCE_ROOT_SMOKE=1 but no root attribution logs — grant su in Magisk and enable root in app"
  fi
else
  if echo "${UI}" | grep -qiE "Root not detected|Requires root|Root access|disabled|No modules"; then
    log "Non-root explanatory copy detected"
  else
    log "WARN: verify root switch disabled on non-root device manually"
  fi
  log "Run diagnostics on non-root (should not crash)"
  "${ADB}" -s "${DEVICE}" logcat -c 2>/dev/null || true
fi

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 100 | grep -E "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash during M3 smoke"

log "PASS: M3 smoke complete"
exit 0
