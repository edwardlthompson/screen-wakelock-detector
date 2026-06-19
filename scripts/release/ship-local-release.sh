#!/usr/bin/env bash
# Build signed release APK locally and publish to GitHub Releases.
#
# Prerequisites:
#   - gh auth login
#   - Release keystore: ~/.screen-wakelock-detector/release.jks
#   - RELEASE_KEY_PASSWORD in .env or environment (see .env.example)
#
# Usage:
#   bash scripts/release/ship-local-release.sh              # version from build.gradle.kts
#   bash scripts/release/ship-local-release.sh v1.2.13
#   SKIP_TAG=1 bash scripts/release/ship-local-release.sh   # upload only (tag must exist)
#   USE_CI=1 bash scripts/release/ship-local-release.sh     # CI build via workflow_dispatch
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[ship-local-release] $*"; }
fail() { echo "[ship-local-release] FAIL: $*" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI required — run: gh auth login"

chmod +x scripts/release/*.sh 2>/dev/null || true

if [[ -n "${1:-}" ]]; then
  TAG="${1#v}"
else
  TAG="$(grep 'versionName' app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')"
fi
TAG="v${TAG}"
VERSION="${TAG#v}"

if [[ "${USE_CI:-0}" == "1" ]]; then
  log "CI mode — dispatch Republish Signed Release for ${TAG}"
  if ! git rev-parse "${TAG}" >/dev/null 2>&1; then
    log "Creating annotated tag ${TAG}"
    git tag -a "${TAG}" -m "Release ${TAG}"
    git push origin "${TAG}"
  elif ! git ls-remote --exit-code --tags origin "${TAG}" >/dev/null 2>&1; then
    git push origin "${TAG}"
  fi
  gh workflow run republish-release.yml -f "tag=${TAG}"
  log "Waiting for workflow..."
  sleep 5
  RUN_ID="$(gh run list --workflow=republish-release.yml --limit 1 --json databaseId -q '.[0].databaseId')"
  gh run watch "${RUN_ID}"
  gh release view "${TAG}" --json url -q .url
  log "PASS: CI release published for ${TAG}"
  exit 0
fi

bash scripts/release/ensure-signing.sh

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  bash scripts/release/build-signed-apk.sh
fi

APK="${ROOT}/dist/Screen-Wakelock-Detector-${VERSION}.apk"
MAPPING="${ROOT}/app/build/outputs/mapping/release/mapping.txt"
[[ -f "${APK}" ]] || fail "APK missing: ${APK}"

NOTES_FILE="$(mktemp)"
bash scripts/release/extract-changelog.sh "${VERSION}" > "${NOTES_FILE}" || echo "See CHANGELOG.md [${VERSION}]" > "${NOTES_FILE}"

if [[ "${SKIP_TAG:-0}" != "1" ]]; then
  if ! git rev-parse "${TAG}" >/dev/null 2>&1; then
    log "Creating annotated tag ${TAG}"
    git tag -a "${TAG}" -m "Release ${TAG}"
  fi
  if ! git ls-remote --exit-code --tags origin "${TAG}" >/dev/null 2>&1; then
    log "Pushing tag ${TAG}"
    git push origin "${TAG}"
  fi
fi

if gh release view "${TAG}" >/dev/null 2>&1; then
  log "Release ${TAG} exists — uploading assets"
  gh release upload "${TAG}" "${APK}" --clobber
  if [[ -f "${MAPPING}" ]]; then
    gh release upload "${TAG}" "${MAPPING}" --clobber
  fi
  if gh release view "${TAG}" --json assets -q '.assets[].name' 2>/dev/null | grep -qx 'app-release-unsigned.apk'; then
    gh release delete-asset "${TAG}" app-release-unsigned.apk --yes || true
  fi
else
  log "Creating GitHub release ${TAG}"
  UPLOAD=( "${APK}" )
  if [[ -f "${MAPPING}" ]]; then
    UPLOAD+=( "${MAPPING}" )
  fi
  gh release create "${TAG}" "${UPLOAD[@]}" \
    --title "Screen Wakelock Detector ${TAG}" \
    --notes-file "${NOTES_FILE}"
fi

rm -f "${NOTES_FILE}"

URL="$(gh release view "${TAG}" --json url -q .url)"
log "Release URL: ${URL}"
log "PASS: ${APK} published to ${TAG}"
