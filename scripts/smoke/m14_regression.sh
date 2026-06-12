#!/usr/bin/env bash
# M14 regression: m14_smoke + memory baseline benchmark (requires authorized device)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[m14_regression] $*"; }
fail() { echo "[m14_regression] FAIL: $*" >&2; exit 1; }

log "Phase 1: m14_smoke"
bash "${SCRIPT_DIR}/m14_smoke.sh"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB:-adb}")" || fail "no authorized device for benchmark"
log "Phase 2: memory baseline on ${DEVICE}"
chmod +x "${ROOT}/scripts/benchmark/memory_baseline.sh"
bash "${ROOT}/scripts/benchmark/memory_baseline.sh"

log "Phase 3: m13 ADB verify (regression coverage)"
if [[ -f "${SCRIPT_DIR}/m13_adb_verify.sh" ]]; then
  bash "${SCRIPT_DIR}/m13_adb_verify.sh"
else
  log "WARN: m13_adb_verify.sh not found — skipping"
fi

log "PASS: m14_regression"
