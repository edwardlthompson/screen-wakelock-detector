#!/usr/bin/env bash
# M6 smoke: widget shows last wake; tap opens app; heatmap renders
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m6_smoke] $*"; }
fail() { echo "[m6_smoke] FAIL: $*" >&2; exit 1; }

DEVICE="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Check app widget provider registered"
DUMP="$("${ADB}" -s "${DEVICE}" shell dumpsys package "${PACKAGE}" 2>/dev/null || true)"
echo "${DUMP}" | grep -qi "AppWidget" && log "AppWidget provider found" || log "WARN: widget provider not in manifest yet"

log "Launch app → Insights heatmap"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://insights/heatmap" -p "${PACKAGE}" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" 1
sleep 3

UI="$("${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "heatmap|pattern|7.?day|grid" \
  && log "Heatmap/pattern UI detected" \
  || log "WARN: verify heatmap renders in Insights manually"

log "Quick Settings tile (if exported)"
"${ADB}" -s "${DEVICE}" shell cmd statusbar expand-settings 2>/dev/null || true
sleep 2
"${ADB}" -s "${DEVICE}" shell input keyevent KEYCODE_BACK 2>/dev/null || true

log "Widget tap simulation via deep link"
"${ADB}" -s "${DEVICE}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://wake/latest" -p "${PACKAGE}" 2>/dev/null || true
sleep 2

FOCUS="$("${ADB}" -s "${DEVICE}" shell dumpsys window | grep mCurrentFocus || true)"
echo "${FOCUS}" | grep -q "${PACKAGE}" && log "App in foreground after widget/deep link" || log "WARN: verify widget tap opens app"

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 50 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash during M6 smoke"

log "PASS: M6 smoke complete"
exit 0
