#!/usr/bin/env bash
# Bump metadata, optionally verify reproducible build, then open fdroiddata MR.
# Used by GitLab fdroiddata-mr and GitHub fdroid-publish workflows.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[prepare-fdroiddata-mr] $*"; }
fail() { echo "[prepare-fdroiddata-mr] FAIL: $*" >&2; exit 1; }

TAG="${CI_COMMIT_TAG:-${GITHUB_REF_NAME:-}}"
TAG="${TAG#refs/tags/}"
COMMIT_REF="${TAG:-HEAD}"
DRY_RUN="${DRY_RUN:-0}"
SKIP_REPRO_VERIFY="${SKIP_REPRO_VERIFY:-0}"
REQUIRE_REPRO_VERIFY="${REQUIRE_REPRO_VERIFY:-0}"
STAMP="${ROOT}/.fdroid-repro-verified"

if [[ "${DRY_RUN}" == "1" ]]; then
  log "DRY RUN — no metadata write, no git push, no MR"
fi

log "Lint metadata"
bash scripts/fdroid/lint-metadata.sh

if [[ "${DRY_RUN}" == "1" ]]; then
  log "Would bump metadata for commit ${COMMIT_REF}"
else
  python3 scripts/fdroid/bump-metadata.py --commit "${COMMIT_REF}"
fi

if [[ "${SKIP_REPRO_VERIFY}" == "1" ]]; then
  log "SKIP_REPRO_VERIFY=1 — reproducible verify skipped"
elif [[ -f "${STAMP}" ]]; then
  log "Reproducible verify stamp present: ${STAMP}"
else
  UP="${UPSTREAM_APK:-app/build/outputs/apk/release/app-release-unsigned.apk}"
  if [[ ! -f "${UP}" ]]; then
    UP="${UPSTREAM_APK:-app/build/outputs/apk/release/app-release.apk}"
  fi
  FDROID_APK="${FDROID_APK:-}"
  if [[ -z "${FDROID_APK}" ]]; then
    for candidate in \
      "${ROOT}"/build/com.screenwakelock.detector/*/app/build/outputs/apk/*/release/*.apk \
      "${ROOT}"/.fdroid-build/*.apk \
      ; do
      if [[ -f "${candidate}" ]]; then
        FDROID_APK="${candidate}"
        break
      fi
    done
  fi

  if [[ -n "${FDROID_APK}" && -f "${FDROID_APK}" && -f "${UP}" ]]; then
    log "Running reproducible verify before MR"
    if [[ "${DRY_RUN}" == "1" ]]; then
      log "Would run verify-reproducible.sh"
    else
      UPSTREAM_APK="${UP}" FDROID_APK="${FDROID_APK}" bash scripts/fdroid/verify-reproducible.sh
      date -u +"%Y-%m-%dT%H:%M:%SZ" > "${STAMP}"
    fi
  elif [[ "${REQUIRE_REPRO_VERIFY}" == "1" ]]; then
    fail "REQUIRE_REPRO_VERIFY=1 but F-Droid or upstream APK missing"
  else
    log "WARN: No F-Droid APK for verify — MR proceeds (set REQUIRE_REPRO_VERIFY=1 to block)"
  fi
fi

if [[ "${DRY_RUN}" == "1" ]]; then
  log "Would run open-fdroiddata-mr.sh"
  log "DONE (dry run)"
  exit 0
fi

[[ -n "${FDROIDDATA_FORK_URL:-}" ]] || fail "Set FDROIDDATA_FORK_URL to your fdroiddata fork"

bash scripts/fdroid/open-fdroiddata-mr.sh
log "DONE"
