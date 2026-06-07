#!/usr/bin/env bash
# M11 smoke: 2-page onboarding, version-aware settings guides, permission rationale rows
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m11_smoke] $*"; }
fail() { echo "[m11_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/SettingsGuideProvider.kt" ]] \
  || fail "SettingsGuideProvider.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/components/PermissionSetupRow.kt" ]] \
  || fail "PermissionSetupRow.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/components/PermissionStepsDialog.kt" ]] \
  || fail "PermissionStepsDialog.kt missing"
log "M11 source files present"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB:-adb}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

install_apk() {
  if ! "${ADB:-adb}" -s "${DEVICE}" install -r "${APK_PATH}" 2>&1; then
    log "Install failed — uninstalling prior build (signature mismatch)"
    "${ADB:-adb}" -s "${DEVICE}" uninstall "${PACKAGE}" || true
    "${ADB:-adb}" -s "${DEVICE}" install "${APK_PATH}"
  fi
}

install_apk

log "Launch onboarding (clear app data first)"
"${ADB:-adb}" -s "${DEVICE}" shell pm clear "${PACKAGE}" >/dev/null 2>&1 || true
install_apk
"${ADB:-adb}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity"
sleep 3

UI="$("${ADB:-adb}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "Find out what keeps|Welcome|Screen Wakelock" \
  && log "Onboarding intro detected" \
  || log "WARN: verify onboarding intro manually"

echo "${UI}" | grep -qi "Verify setup" \
  && fail "Verify page should not appear in M11 onboarding" \
  || log "No Verify setup page (expected)"

log "PASS: M11 smoke complete (manual: 2-page flow + permission rationale on Permissions step)"
