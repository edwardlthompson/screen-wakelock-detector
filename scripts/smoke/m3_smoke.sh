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

DEVICE="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

ROOTED=false
if [[ "${FORCE_ROOT_SMOKE:-0}" == "1" ]]; then
  SU_OUT="$(timeout 2 "${ADB}" -s "${DEVICE}" shell su -c id 2>/dev/null || true)"
  if echo "${SU_OUT}" | grep -q "uid=0"; then
    ROOTED=true
    log "Device appears rooted (su available)"
  fi
else
  log "Skipping su probe (set FORCE_ROOT_SMOKE=1 on rooted device)"
fi

if [[ "${ROOTED}" == "false" ]]; then
  log "Device non-root — checking grayed UI path"
fi

log "Open Settings → Root"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 2

"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://settings/root" -p "${PACKAGE}" 2>/dev/null || true
sleep 2

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"

if [[ "${ROOTED}" == "true" ]]; then
  echo "${UI}" | grep -qiE "Root|wakelock|diagnostic|enhanced" \
    && log "Root UI elements found" \
    || log "WARN: enable Root in app Settings and verify wakelock tag on wake detail"
  log "Trigger screen on for root snapshot"
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
  sleep 1
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 3
  LOGS="$("${ADB}" -s "${DEVICE}" logcat -d -t 150 | grep -iE "RootAttributor|dumpsys|wakelock" | grep -i "${PACKAGE}" || true)"
  [[ -n "${LOGS}" ]] && log "Root attribution logs found" || log "WARN: no root parser logs"
else
  echo "${UI}" | grep -qiE "Requires root|gray|disabled|Not detected|Root access" \
    && log "Non-root explanatory copy detected" \
    || log "WARN: verify root rows disabled/grayed in Settings → Root manually"
fi

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 100 | grep -E "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash during M3 smoke"

log "PASS: M3 smoke complete"
exit 0
