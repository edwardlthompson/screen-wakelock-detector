#!/usr/bin/env bash
# Compare upstream release APK hash vs F-Droid build output (reproducible verify)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

UPSTREAM_APK="${UPSTREAM_APK:-app/build/outputs/apk/release/app-release.apk}"
FDROID_APK="${FDROID_APK:-}"
ALLOW_MISMATCH="${ALLOW_MISMATCH:-0}"

log() { echo "[verify-reproducible] $*"; }
fail() { echo "[verify-reproducible] FAIL: $*" >&2; exit 1; }

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    fail "sha256sum or shasum required"
  fi
}

[[ -f "${UPSTREAM_APK}" ]] || fail "Upstream APK not found: ${UPSTREAM_APK} (run assembleRelease first)"

if [[ -z "${FDROID_APK}" ]]; then
  # Search common F-Droid CI output paths
  for candidate in \
    "${ROOT}/build/com.screenwakelock.detector/"*"/app/build/outputs/apk/"*"/release/"*.apk \
    "${ROOT}/.fdroid-build/"*.apk \
    ; do
    if [[ -f "${candidate}" ]]; then
      FDROID_APK="${candidate}"
      break
    fi
  done
fi

[[ -n "${FDROID_APK}" && -f "${FDROID_APK}" ]] || fail "F-Droid APK not found — set FDROID_APK env var"

UP_HASH="$(sha256_file "${UPSTREAM_APK}")"
FD_HASH="$(sha256_file "${FDROID_APK}")"

log "Upstream: ${UPSTREAM_APK}"
log "  SHA256: ${UP_HASH}"
log "F-Droid:  ${FDROID_APK}"
log "  SHA256: ${FD_HASH}"

if [[ "${UP_HASH}" == "${FD_HASH}" ]]; then
  log "PASS: APK hashes match (byte-identical before signature compare)"
  exit 0
fi

if [[ "${ALLOW_MISMATCH}" == "1" ]]; then
  log "WARN: hash mismatch allowed (ALLOW_MISMATCH=1)"
  exit 0
fi

if command -v apksigcopier >/dev/null 2>&1; then
  log "Hashes differ — try apksigcopier for signature-aware compare"
  TMP="${TMPDIR:-/tmp}/fdroid-unsigned-$$.apk"
  apksigcopier copy "${UPSTREAM_APK}" "${TMP}" 2>/dev/null || true
  if [[ -f "${TMP}" ]]; then
    U2="$(sha256_file "${TMP}")"
    rm -f "${TMP}"
    if [[ "${U2}" == "${FD_HASH}" ]]; then
      log "PASS: unsigned content matches after apksigcopier"
      exit 0
    fi
  fi
fi

if command -v diffoscope >/dev/null 2>&1; then
  log "Running diffoscope (first 50 lines)"
  diffoscope "${UPSTREAM_APK}" "${FDROID_APK}" 2>&1 | head -50 || true
fi

fail "Reproducible verify failed — see docs/F-DROID.md and F-Droid wiki HOWTO"
exit 1
