#!/usr/bin/env bash
# Lint F-Droid metadata and YAML syntax
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
METADATA="${ROOT}/fdroid/metadata/com.screenwakelock.detector.yml"
FDROID_YML="${ROOT}/.fdroid.yml"

log() { echo "[lint-metadata] $*"; }
fail() { echo "[lint-metadata] FAIL: $*" >&2; exit 1; }

[[ -f "${METADATA}" ]] || fail "missing ${METADATA}"
[[ -f "${FDROID_YML}" ]] || fail "missing ${FDROID_YML}"

if command -v yamllint >/dev/null 2>&1; then
  log "Running yamllint"
  yamllint -d "{extends: relaxed, rules: {line-length: {max: 200}}}" "${METADATA}" "${FDROID_YML}"
else
  log "yamllint not installed — skipping (install via pip install yamllint)"
fi

if command -v fdroid >/dev/null 2>&1; then
  log "Running fdroid lint"
  (cd "${ROOT}" && fdroid lint "${METADATA}")
else
  log "fdroid CLI not installed — basic field checks only"
fi

# Required fields
for field in AutoName License CurrentVersion CurrentVersionCode Categories; do
  grep -q "^${field}:" "${METADATA}" || fail "missing field ${field} in metadata"
done

grep -q "^Builds:" "${METADATA}" || fail "missing Builds section"

# Policy checks
grep -qi "Apache-2.0" "${METADATA}" || fail "License must be Apache-2.0"

log "PASS: metadata lint"
exit 0
