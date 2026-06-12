#!/usr/bin/env bash
# Quick verify on secondary device (Essential PH-1 / older hardware).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"
ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || exit 1
ADB_S=( "${ADB}" -s "${DEVICE}" )
log() { echo "[ph1_verify] $*"; }
MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
log "Device ${DEVICE} (${MODEL}, API ${SDK})"
"${ADB_S[@]}" shell dumpsys package "${PACKAGE}" | grep -q "versionName=1.2.0" && log "PASS: version 1.2.0 installed"
LISTENERS="$("${ADB_S[@]}" shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
echo "${LISTENERS}" | grep -q "${PACKAGE}" && log "PASS: notification listener granted" || log "WARN: notification listener not granted — grant in Settings for attribution"
"${ADB_S[@]}" logcat -c 2>/dev/null || true
"${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
sleep 2
"${ADB_S[@]}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || "${ADB_S[@]}" shell input keyevent KEYCODE_POWER
sleep 1
"${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || "${ADB_S[@]}" shell input keyevent KEYCODE_POWER
sleep 3
LOGS="$("${ADB_S[@]}" logcat -d -s WakeMonitor:I 2>/dev/null || true)"
echo "${LOGS}" | grep -q "WakeEvent inserted" && log "PASS: wake capture working" || log "WARN: no WakeEvent in logcat"
for route in "screenwakelock://settings" "screenwakelock://insights" "screenwakelock://app/permissions"; do
  "${ADB_S[@]}" shell am start -a android.intent.action.VIEW -d "${route}" -p "${PACKAGE}" >/dev/null 2>&1 || true
  sleep 2
done
CRASH="$("${ADB_S[@]}" logcat -d -t 80 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] && log "PASS: no crashes on Settings/Insights/Permissions" || { log "FAIL: crash detected"; exit 1; }
UI="$("${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "permission|notification|usage" && log "PASS: Permissions screen reachable"
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW -d "screenwakelock://insights" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 3
UI2="$("${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI2}" | grep -qiE "insights|Total|offender|week" && log "PASS: Insights screen renders" || log "WARN: Insights UI not matched — may need data or scroll"
log "DONE: PH-1 verification complete"
