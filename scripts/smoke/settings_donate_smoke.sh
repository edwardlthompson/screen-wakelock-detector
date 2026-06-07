#!/usr/bin/env bash
# Smoke: Settings About → Venmo donate link opens Venmo, browser, or chooser (not in-app error).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
DONATE_URL="${DONATE_URL:-https://venmo.com/code?user_id=1857304970395648420}"

log() { echo "[settings_donate_smoke] $*"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
ADB_S=( "${ADB}" -s "${DEVICE}" )

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null

log "Device ${DEVICE}"

"${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
"${ADB_S[@]}" logcat -c 2>/dev/null || true

log "Open donate link via debug deep link (Settings About automation)"
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://settings/donate?automation=open" -p "${PACKAGE}" >/dev/null
sleep 2

FOCUS="$("${ADB_S[@]}" shell dumpsys window | grep mCurrentFocus | tr -d '\r' || true)"
STACK="$("${ADB_S[@]}" shell dumpsys activity activities | tr -d '\r' || true)"
log "Foreground after donate tap: ${FOCUS}"

if echo "${FOCUS}" | grep -qiE "venmo|ResolverActivity|Chrome|Firefox|Browser|WebView"; then
  pass "Donate link left app for Venmo, chooser, or browser"
elif echo "${STACK}" | grep -qi "com\.venmo"; then
  pass "Venmo activity started (in task stack)"
elif echo "${FOCUS}" | grep -q "${PACKAGE}"; then
  LOGS="$("${ADB_S[@]}" logcat -d -t 120 | tr -d '\r' || true)"
  if echo "${LOGS}" | grep -qi "about_no_handler\|No app found to open"; then
    fail "App still foreground and link-open-failed snackbar logged"
  fi
  fail "App still foreground — donate link did not open an external handler (${FOCUS})"
else
  pass "External activity in foreground (${FOCUS})"
fi

ERR_SNACK="$("${ADB_S[@]}" logcat -d -t 120 | grep -i "about_no_handler\|No app found to open" || true)"
[[ -z "${ERR_SNACK}" ]] && pass "No link-open-failed message in logcat" \
  || fail "Link open failed message in logcat: ${ERR_SNACK}"

log "Direct Venmo package intent resolves on device"
RESOLVED="$("${ADB_S[@]}" shell cmd package resolve-activity --brief \
  -a android.intent.action.VIEW -d "${DONATE_URL}" com.venmo 2>/dev/null | tr -d '\r' || true)"
[[ -n "${RESOLVED}" ]] && pass "Venmo handler: ${RESOLVED}" || log "WARN: Venmo package intent did not resolve (browser fallback expected)"

log "settings_donate_smoke PASS on ${DEVICE}"
exit 0
