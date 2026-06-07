#!/usr/bin/env bash
# M9 smoke: install signed release APK, launch app, verify versionName.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"

log() { echo "[m9_smoke] $*"; }
fail() { echo "[m9_smoke] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

VERSION="$(grep 'versionName' app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')"
APK_PATH="${APK_PATH:-dist/Screen-Wakelock-Detector-${VERSION}.apk}"

if [[ ! -f "${APK_PATH}" ]]; then
  log "Signed APK missing — running build-signed-apk.sh"
  bash scripts/release/build-signed-apk.sh
fi
[[ -f "${APK_PATH}" ]] || fail "Signed APK not found: ${APK_PATH}"

bash scripts/release/verify-signed-apk.sh "${APK_PATH}"

log "Install signed release APK"
if ! "${ADB}" -s "${DEVICE}" install -r "${APK_PATH}" 2>&1; then
  log "Install failed — uninstalling prior build (debug vs release signature)"
  "${ADB}" -s "${DEVICE}" uninstall "${PACKAGE}" || true
  "${ADB}" -s "${DEVICE}" install "${APK_PATH}"
fi

log "Launch MainActivity"
"${ADB}" -s "${DEVICE}" shell am start -n "${PACKAGE}/.MainActivity"
sleep 3

INSTALLED_VERSION="$("${ADB}" -s "${DEVICE}" shell dumpsys package "${PACKAGE}" 2>/dev/null \
  | grep -m1 'versionName=' | sed 's/.*versionName=//' | tr -d '\r' || true)"
log "Installed versionName=${INSTALLED_VERSION}"
echo "${INSTALLED_VERSION}" | grep -q "${VERSION}" \
  || fail "Expected version ${VERSION}, got ${INSTALLED_VERSION}"

CRASH="$("${ADB}" -s "${DEVICE}" logcat -d -t 80 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash after launch"

log "PASS: M9 smoke complete (signed ${VERSION} on ${DEVICE})"
exit 0
