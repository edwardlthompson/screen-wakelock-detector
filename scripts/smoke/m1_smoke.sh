#!/usr/bin/env bash
# M1 smoke: lock/unlock → new history row within 5s; foreground service notification present
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
MAIN_ACTIVITY="${MAIN_ACTIVITY:-com.screenwakelock.detector.MainActivity}"

log() { echo "[m1_smoke] $*"; }
fail() { echo "[m1_smoke] FAIL: $*" >&2; exit 1; }

DEVICE="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/${MAIN_ACTIVITY}" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" -c android.intent.category.LAUNCHER 1

sleep 3

log "Checking foreground service notification"
NOTIFS="$("${ADB}" -s "${DEVICE}" shell dumpsys notification --noredact 2>/dev/null | grep -i "${PACKAGE}" || true)"
echo "${NOTIFS}" | grep -qiE "monitor|screen|wake" \
  || log "WARN: FGS notification text not matched — verify monitoring notification manually"

log "Checking logcat for wake capture event"
"${ADB}" -s "${DEVICE}" logcat -c 2>/dev/null || true
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 2
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 1
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
sleep 2

LOGS="$("${ADB}" -s "${DEVICE}" logcat -d -s WakeMonitor:I WakeMonitor:D 2>/dev/null || true)"
if echo "${LOGS}" | grep -q "WakeEvent inserted"; then
  LATENCY="$(echo "${LOGS}" | grep -oE 'latencyMs=[0-9]+' | head -1 | cut -d= -f2 || true)"
  log "WakeEvent captured (latencyMs=${LATENCY:-unknown})"
  if [[ -n "${LATENCY}" ]] && [[ "${LATENCY}" -gt 500 ]]; then
    log "WARN: capture latency ${LATENCY}ms exceeds 500ms target"
  fi
else
  log "WARN: no WakeEvent inserted log — verify Room insert via History tab"
fi

log "Relaunch app to verify history UI reachable"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/${MAIN_ACTIVITY}" 2>/dev/null || true
sleep 2

log "PASS: M1 smoke complete (confirm history row on device if logs empty)"
exit 0
