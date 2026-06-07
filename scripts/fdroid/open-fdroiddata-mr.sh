#!/usr/bin/env bash
# Push metadata bump to fdroiddata fork and open merge request via glab
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
METADATA_SRC="${ROOT}/fdroid/metadata/com.screenwakelock.detector.yml"
APP_ID="com.screenwakelock.detector"

log() { echo "[open-fdroiddata-mr] $*"; }
fail() { echo "[open-fdroiddata-mr] FAIL: $*" >&2; exit 1; }

FDROIDDATA_FORK_URL="${FDROIDDATA_FORK_URL:-}"
GITLAB_TOKEN="${GITLAB_TOKEN:-}"

[[ -n "${FDROIDDATA_FORK_URL}" ]] || fail "Set FDROIDDATA_FORK_URL (e.g. git@gitlab.com:you/fdroiddata.git)"
command -v git >/dev/null 2>&1 || fail "git required"
[[ -f "${METADATA_SRC}" ]] || fail "missing source metadata"

VERSION_NAME="$(grep -E '^CurrentVersion:' "${METADATA_SRC}" | awk '{print $2}')"
BRANCH="screenwakelock-${VERSION_NAME}-$(date +%Y%m%d)"

WORK="${TMPDIR:-/tmp}/fdroiddata-mr-$$"
mkdir -p "${WORK}"
trap 'rm -rf "${WORK}"' EXIT

log "Cloning fdroiddata fork"
git clone --depth 1 "${FDROIDDATA_FORK_URL}" "${WORK}/fdroiddata"
cd "${WORK}/fdroiddata"

git checkout -b "${BRANCH}"
mkdir -p metadata
cp "${METADATA_SRC}" "metadata/${APP_ID}.yml"
git add "metadata/${APP_ID}.yml"
git commit -m "Update ${APP_ID} to ${VERSION_NAME}"

log "Pushing branch ${BRANCH}"
git push origin "${BRANCH}"

if command -v glab >/dev/null 2>&1; then
  log "Creating merge request via glab"
  glab mr create \
    --repo fdroid/fdroiddata \
    --source-branch "${BRANCH}" \
    --title "Update ${APP_ID} to ${VERSION_NAME}" \
    --description "Automated MR from Screen Wakelock Detector CI. Version ${VERSION_NAME}." \
    --yes || log "WARN: glab mr create failed — open MR manually on GitLab"
else
  log "glab not found — push complete; open MR manually to fdroid/fdroiddata"
fi

log "DONE"
exit 0
