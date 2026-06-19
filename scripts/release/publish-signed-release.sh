#!/usr/bin/env bash
# Build signed APK and upload to an existing GitHub release tag.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

TAG="${1:-${RELEASE_TAG:-v1.2.1}}"
TAG="${TAG#v}"
TAG="v${TAG}"

log() { echo "[publish-signed-release] $*"; }
fail() { echo "[publish-signed-release] FAIL: $*" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI not found"

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  bash scripts/release/build-signed-apk.sh
fi

VERSION="$(grep 'versionName' app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')"
APK="${ROOT}/dist/Screen-Wakelock-Detector-${VERSION}.apk"
[[ -f "${APK}" ]] || fail "APK missing: ${APK}"

ASSET_NAME="Screen-Wakelock-Detector-${VERSION}.apk"

if gh release view "${TAG}" >/dev/null 2>&1; then
  if gh release view "${TAG}" --json assets -q '.assets[].name' 2>/dev/null | grep -qx 'app-release-unsigned.apk'; then
    log "Removing unsigned asset from ${TAG}"
    gh release delete-asset "${TAG}" app-release-unsigned.apk --yes || true
  fi
else
  log "Release ${TAG} not found — creating with changelog notes"
  NOTES_FILE="$(mktemp)"
  bash scripts/release/extract-changelog.sh "${VERSION}" > "${NOTES_FILE}" \
    || echo "See CHANGELOG.md [${VERSION}]" > "${NOTES_FILE}"
  gh release create "${TAG}" --title "Screen Wakelock Detector ${TAG}" --notes-file "${NOTES_FILE}"
  rm -f "${NOTES_FILE}"
fi

log "Uploading ${ASSET_NAME} to ${TAG}"
gh release upload "${TAG}" "${APK}" --clobber

log "PASS: published ${ASSET_NAME} to ${TAG}"
