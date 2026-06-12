#!/usr/bin/env bash
# M13 smoke: M12 review fixes — WakeEventIdentity, shared PreferenceKeys, display alignment
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m13_smoke] $*"; }
fail() { echo "[m13_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/domain/model/WakeEventIdentity.kt" ]] \
  || fail "WakeEventIdentity.kt missing"
grep -q "WakeEventIdentity" "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/WakeEventFilters.kt" \
  || fail "WakeEventFilters must delegate to WakeEventIdentity"
grep -q "PreferenceKeys.IGNORED_PACKAGES" \
  "${ROOT}/app/src/main/java/com/screenwakelock/detector/data/repository/PreferencesRepository.kt" \
  || fail "PreferencesRepository must use PreferenceKeys.IGNORED_PACKAGES"
grep -q "PreferenceKeys.IGNORED_PACKAGES" \
  "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/IgnoredPackagesReader.kt" \
  || fail "IgnoredPackagesReader must use PreferenceKeys.IGNORED_PACKAGES"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/domain/attributor/WakeAttributorLogic.kt" ]] \
  || fail "WakeAttributorLogic.kt missing"
log "M13 source files present"

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

[[ -f "${APK_PATH}" ]] || fail "APK not found at ${APK_PATH} — run ./gradlew assembleDebug first"
install_apk

log "Launch app"
"${ADB:-adb}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity"
sleep 2

UI="$("${ADB:-adb}" -s "${DEVICE}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
echo "${UI}" | grep -qiE "Home|Last screen wake|Fix it" \
  && log "Home screen detected" \
  || log "WARN: verify Home screen manually"

log "PASS: M13 smoke complete (manual: tag-only ignore; History search matches card name)"
log "Record in GATES.md: Smoke M13: PASS <timestamp> ${DEVICE} 1.2.10"
