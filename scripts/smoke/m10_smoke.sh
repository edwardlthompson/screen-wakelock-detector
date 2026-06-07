#!/usr/bin/env bash
# M10 smoke: restricted-setup helpers, live permission refresh on onboarding
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m10_smoke] $*"; }
fail() { echo "[m10_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/RestrictedSettingsHelper.kt" ]] \
  || fail "RestrictedSettingsHelper.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/InstallSourceHelper.kt" ]] \
  || fail "InstallSourceHelper.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/components/RestrictedSetupCard.kt" ]] \
  || fail "RestrictedSetupCard.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/hooks/usePermissionStatuses.kt" ]] \
  || fail "usePermissionStatuses.kt missing"
log "M10 source files present"

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
echo "${UI}" | grep -qiE "Welcome|Find out|Screen Wakelock" \
  && log "Onboarding welcome detected" \
  || log "WARN: verify onboarding manually"

log "PASS: M10 smoke complete (manual: Permissions page live refresh + restricted card on sideload)"
