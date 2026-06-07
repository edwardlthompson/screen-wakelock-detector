#!/usr/bin/env bash
# Build signed release APK and copy to dist/Screen-Wakelock-Detector-{version}.apk
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[build-signed-apk] $*"; }
fail() { echo "[build-signed-apk] FAIL: $*" >&2; exit 1; }

chmod +x scripts/release/*.sh 2>/dev/null || true

if [[ -n "${RELEASE_STORE_FILE:-}" && -f "${RELEASE_STORE_FILE}" ]]; then
  log "Using RELEASE_STORE_FILE from environment (CI)"
elif [[ -f "${ROOT}/keystore.properties" ]]; then
  log "Using keystore.properties"
else
  bash scripts/release/setup-keystore.sh
fi

if [[ -z "${JAVA_HOME:-}" && -d "/c/Program Files/Android/Android Studio/jbr" ]]; then
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
fi

log "assembleRelease + copyNamedReleaseApk"
./gradlew assembleRelease copyNamedReleaseApk

export EXPECT_SIGNED=1
bash scripts/release/verify-release-apk.sh

VERSION="$(./gradlew -q printVersionName 2>/dev/null || true)"
if [[ -z "${VERSION}" ]]; then
  VERSION="$(grep 'versionName' app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')"
fi
NAMED_APK="${ROOT}/dist/Screen-Wakelock-Detector-${VERSION}.apk"
[[ -f "${NAMED_APK}" ]] || fail "Named APK missing: ${NAMED_APK}"

bash scripts/release/verify-signed-apk.sh "${NAMED_APK}"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "${NAMED_APK}"
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "${NAMED_APK}"
fi

log "Output: ${NAMED_APK}"
log "Install: adb install -r dist/Screen-Wakelock-Detector-${VERSION}.apk"
log "PASS: signed release build complete"
