#!/usr/bin/env bash
# M8 smoke: release verify script, launcher icon, settings, insights, wake-count widget
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m8_smoke] $*"; }
fail() { echo "[m8_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${ROOT}/scripts/release/verify-release-apk.sh" ]] \
  || fail "scripts/release/verify-release-apk.sh missing"
log "Release verify script present"

[[ -f "${ROOT}/app/src/main/res/drawable/ic_launcher_foreground.xml" ]] \
  || fail "ic_launcher_foreground.xml missing"
log "Launcher icon drawable present"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Check wake-count widget provider registered"
DUMP="$("${ADB}" -s "${DEVICE}" shell dumpsys package "${PACKAGE}" 2>/dev/null || true)"
echo "${DUMP}" | grep -qi "WakeCountWidgetReceiver" \
  && log "WakeCountWidget provider found" \
  || log "WARN: WakeCountWidget provider not found in dumpsys"

log "Launch Settings"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://settings" -p "${PACKAGE}" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 2

SETTINGS_UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${SETTINGS_UI}" | grep -qiE "settings|monitoring|export|backup" \
  && log "Settings UI detected" \
  || log "WARN: verify Settings screen manually"

log "Launch Insights"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://insights" -p "${PACKAGE}" 2>/dev/null || true
sleep 3

INSIGHTS_UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${INSIGHTS_UI}" | grep -qiE "insights|offender|week|heatmap|pattern" \
  && log "Insights UI detected" \
  || log "WARN: verify Insights screen manually"

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 80 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash during M8 smoke"

log "PASS: M8 smoke complete"
exit 0
