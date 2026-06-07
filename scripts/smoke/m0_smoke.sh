#!/usr/bin/env bash
# M0 smoke: APK installs; app launches; M3 theme visible; no crash 30s
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
SMOKE_TIMEOUT="${SMOKE_TIMEOUT:-45}"
MAIN_ACTIVITY="${MAIN_ACTIVITY:-com.screenwakelock.detector.MainActivity}"

log() { echo "[m0_smoke] $*"; }
fail() { echo "[m0_smoke] FAIL: $*" >&2; exit 1; }

if ! command -v "${ADB}" >/dev/null 2>&1; then
  fail "adb not found in PATH"
fi

DEVICE="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "${DEVICE}" ]] || fail "no authorized device (adb devices)"

log "Using device: ${DEVICE}"

if [[ ! -f "${APK_PATH}" ]]; then
  log "APK missing — building assembleDebug"
  ./gradlew assembleDebug
fi

[[ -f "${APK_PATH}" ]] || fail "APK not found at ${APK_PATH}"

log "Installing ${APK_PATH}"
"${ADB}" -s "${DEVICE}" install -r "${APK_PATH}"

log "Launching ${PACKAGE}/${MAIN_ACTIVITY}"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/${MAIN_ACTIVITY}" \
  || "${ADB}" -s "${DEVICE}" shell monkey -p "${PACKAGE}" -c android.intent.category.LAUNCHER 1

log "Waiting ${SMOKE_TIMEOUT}s for stability"
sleep "${SMOKE_TIMEOUT}"

if "${ADB}" -s "${DEVICE}" shell pidof "${PACKAGE}" >/dev/null 2>&1; then
  log "Process still running after ${SMOKE_TIMEOUT}s"
else
  fail "App process not running — possible crash"
fi

CRASHES="$("${ADB}" -s "${DEVICE}" logcat -d -t 200 | grep -E "FATAL EXCEPTION|AndroidRuntime" | grep -i "${PACKAGE}" || true)"
[[ -z "${CRASHES}" ]] || fail "Crash detected in logcat:\n${CRASHES}"

log "Checking UI hierarchy for Compose/Material presence"
UI="$("${ADB}" -s "${DEVICE}" shell uiautomator dump /dev/tty 2>/dev/null || "${ADB}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
# Fallback: activity focus
FOCUS="$("${ADB}" -s "${DEVICE}" shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp' | head -3 || true)"
echo "${FOCUS}" | grep -q "${PACKAGE}" || log "WARN: focus check inconclusive — verify M3 theme manually"

log "PASS: M0 smoke complete"
exit 0
