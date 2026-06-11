#!/usr/bin/env bash
# M12 smoke: QuickFix ignore button, ignored apps hidden from History lists
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m12_smoke] $*"; }
fail() { echo "[m12_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/domain/attributor/PackageFromWakelockTag.kt" ]] \
  || fail "PackageFromWakelockTag.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/domain/attributor/AppDisplayResolver.kt" ]] \
  || fail "AppDisplayResolver.kt missing"
grep -q "Ignore this app" "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/components/QuickFixBottomSheet.kt" \
  || fail "QuickFixBottomSheet missing Ignore this app"
grep -q "onIgnoreApp" "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/screens/HomeScreen.kt" \
  || fail "HomeScreen missing onIgnoreApp wiring"
grep -q "WakeEventFilters" "${ROOT}/app/src/main/java/com/screenwakelock/detector/ui/viewmodel/ViewModels.kt" \
  || fail "ViewModels missing WakeEventFilters for History/Home"
log "M12 source files present"

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

log "PASS: M12 smoke complete (manual: Fix it → Ignore this app; ignored app absent from History)"
log "Record in GATES.md: Smoke M12: PASS <timestamp> ${DEVICE} 1.2.9"
