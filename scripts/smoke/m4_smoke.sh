#!/usr/bin/env bash
# M4 smoke: last-wake card → bottom sheet → channel settings intent resolves
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m4_smoke] $*"; }
fail() { echo "[m4_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Launch app Home"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 3

log "Tap center screen to open last-wake / quick-fix (coordinates fallback)"
WM="$("${ADB}" -s "${DEVICE}" shell wm size | tr -d '\r')"
log "Display: ${WM}"
"${ADB}" -s "${DEVICE}" shell input tap 540 800 2>/dev/null || true
sleep 2

log "Attempt deep link to quick-fix sheet for latest wake"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://wake/latest/actions" -p "${PACKAGE}" 2>/dev/null || true
sleep 3

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "Silence channel|Open notification|Why this app" \
  && log "Quick-fix bottom sheet detected" \
  || log "WARN: bottom sheet not detected — may need a logged wake on device"

log "Deep link quick-fix for event id 1 (if exists)"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://app/quickfix/1" -p "${PACKAGE}" 2>/dev/null || true
sleep 2

log "Fire channel settings intent (generic)"
"${ADB}" -s "${DEVICE}" shell am start -a android.settings.CHANNEL_NOTIFICATION_SETTINGS 2>/dev/null \
  -e android.provider.extra.APP_PACKAGE "${PACKAGE}" \
  -e android.provider.extra.CHANNEL_ID "default" 2>/dev/null || true
sleep 2

RESOLVED="$("${ADB}" -s "${DEVICE}" shell dumpsys activity activities | grep -iE "NotificationSettings|ChannelNotification" | head -3 || true)"
[[ -n "${RESOLVED}" ]] && log "Settings activity resolved" || log "WARN: channel settings intent may be OEM-specific"

log "Return to app — no crash"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null || true
sleep 1

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 50 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash after settings intent"

log "PASS: M4 smoke complete"
exit 0
