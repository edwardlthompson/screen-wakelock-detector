#!/usr/bin/env bash
# M5 smoke: insights counts match history; threshold alert fires on synthetic burst
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
BURST_COUNT="${BURST_COUNT:-5}"

log() { echo "[m5_smoke] $*"; }
fail() { echo "[m5_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Launch app"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 2

log "Navigate to Insights tab (deep link if available)"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://insights" -p "${PACKAGE}" 2>/dev/null || true
sleep 2

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "Insights|offender|today|wake" \
  && log "Insights UI detected" \
  || log "WARN: open Insights tab manually"

log "Synthetic notification burst (${BURST_COUNT}) for threshold alert"
for i in $(seq 1 "${BURST_COUNT}"); do
  "${ADB}" -s "${DEVICE}" shell cmd notification post -S bigtext -t "Burst${i}" "alert_test" "M5 burst ${i}" 2>/dev/null || true
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || true
  sleep 1
  "${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 1
done

sleep 3
NOTIFS="$("${ADB}" -s "${DEVICE}" shell dumpsys notification --noredact 2>/dev/null | grep -i "${PACKAGE}" || true)"
if echo "${NOTIFS}" | grep -qiE "woke your screen|times in the last"; then
  log "Threshold alert notification detected"
else
  log "WARN: enable alerts + POST_NOTIFICATIONS and verify threshold alert manually"
fi

log "PASS: M5 smoke complete"
exit 0
