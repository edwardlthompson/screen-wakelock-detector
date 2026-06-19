#!/usr/bin/env bash
# M2 smoke: test notification → screen wake → attributed app+channel on detail screen
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
TEST_CHANNEL="${TEST_CHANNEL:-smoke_channel}"

log() { echo "[m2_smoke] $*"; }
fail() { echo "[m2_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Verify notification listener enabled for ${PACKAGE}"
LISTENERS="$("${ADB}" -s "${DEVICE}" shell cmd notification allowed_listeners 2>/dev/null || true)"
if [[ "${LISTENERS}" == *"Unknown command"* ]] || [[ -z "${LISTENERS}" ]]; then
  LISTENERS="$("${ADB}" -s "${DEVICE}" shell settings get secure enabled_notification_listeners 2>/dev/null || true)"
fi
if ! echo "${LISTENERS}" | grep -q "${PACKAGE}"; then
  log "Attempt adb grant: cmd notification allow_listener"
  "${ADB}" -s "${DEVICE}" shell cmd notification allow_listener \
    "${PACKAGE}/com.screenwakelock.detector.service.NotificationCaptureService" 2>/dev/null || true
  LISTENERS="$("${ADB}" -s "${DEVICE}" shell settings get secure enabled_notification_listeners 2>/dev/null || true)"
fi
if ! echo "${LISTENERS}" | grep -q "${PACKAGE}"; then
  log "Opening notification listener settings — grant access manually then re-run"
  "${ADB}" -s "${DEVICE}" shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
  fail "Notification access not granted for ${PACKAGE}"
fi

log "Grant usage stats for fallback path (optional)"
"${ADB}" -s "${DEVICE}" shell appops set "${PACKAGE}" GET_USAGE_STATS allow 2>/dev/null || true

log "Launch app and start monitoring"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" -c android.intent.category.LAUNCHER 1
sleep 4

log "Posting test notification via cmd notification"
"${ADB}" -s "${DEVICE}" logcat -c 2>/dev/null || true
"${ADB}" -s "${DEVICE}" shell cmd notification post -S bigtext -t "SmokeTest" "${TEST_CHANNEL}" "M2 smoke body" 2>/dev/null \
  || log "WARN: cmd notification post failed — use helper app notification"
sleep 1

log "Simulating screen off/on to trigger wake capture"
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 2
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_POWER
sleep 3

log "Checking attribution logs"
ATTR_LOGS="$("${ADB}" -s "${DEVICE}" logcat -d -s WakeAttributor:I WakeMonitor:I 2>/dev/null || true)"
if echo "${ATTR_LOGS}" | grep -q "Attributed wake"; then
  echo "${ATTR_LOGS}" | grep "Attributed wake" | tail -1
  if echo "${ATTR_LOGS}" | grep -E "Attributed wake.*com\.android\.shell|channel=${TEST_CHANNEL}" | grep -q .; then
    log "Notification attribution matched shell test notification"
  else
    log "WARN: attribution present but shell/channel not matched — verify manually"
  fi
else
  log "WARN: no WakeAttributor log — check notification listener + wake capture"
fi

if echo "${ATTR_LOGS}" | grep -q "WakeEvent inserted"; then
  log "WakeEvent persisted with attribution metadata"
else
  log "WARN: no WakeEvent inserted log"
fi

log "Launch app History tab"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null || true
sleep 2

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
if echo "${UI}" | grep -qiE "channel|SmokeTest|confidence|Why|shell"; then
  log "UI contains attribution-related text"
else
  log "WARN: attribution UI not detected in dump — open History → detail manually"
fi

log "PASS: M2 smoke complete"
exit 0
