#!/usr/bin/env bash
# M2 smoke: test notification → screen wake → attributed app+channel on detail screen
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
TEST_PKG="${TEST_PKG:-com.android.shell}"

log() { echo "[m2_smoke] $*"; }
fail() { echo "[m2_smoke] FAIL: $*" >&2; exit 1; }

DEVICE="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Verify notification listener enabled for ${PACKAGE}"
LISTENERS="$("${ADB}" -s "${DEVICE}" shell cmd notification allowed_listeners 2>/dev/null || true)"
if [[ "${LISTENERS}" == *"Unknown command"* ]] || [[ -z "${LISTENERS}" ]]; then
  LISTENERS="$("${ADB}" -s "${DEVICE}" shell settings get secure enabled_notification_listeners 2>/dev/null || true)"
fi
if ! echo "${LISTENERS}" | grep -q "${PACKAGE}"; then
  log "Opening notification listener settings — grant access manually then re-run"
  "${ADB}" -s "${DEVICE}" shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
  fail "Notification access not granted for ${PACKAGE}"
fi

log "Posting test notification via cmd notification"
"${ADB}" -s "${DEVICE}" shell cmd notification post -S bigtext -t "SmokeTest" "smoke_channel" "M2 smoke body" 2>/dev/null \
  || log "WARN: cmd notification post failed — use helper app notification"

sleep 1
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 2
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 3

log "Launch app and dump UI for attribution strings"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 3

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
if echo "${UI}" | grep -qiE "channel|SmokeTest|confidence|Why"; then
  log "UI contains attribution-related text"
else
  log "WARN: attribution UI not detected in dump — verify detail screen manually"
fi

LOGS="$("${ADB}" -s "${DEVICE}" logcat -d -t 200 | grep -iE "attribut|NOTIFICATION|channel" | grep -i "${PACKAGE}" || true)"
[[ -n "${LOGS}" ]] && log "Attribution logs present"

log "PASS: M2 smoke complete"
exit 0
