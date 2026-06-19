#!/usr/bin/env bash
# M14 regression: m14_smoke + memory baseline benchmark (requires authorized device)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
export ADB="$(resolve_smoke_adb)"

log() { echo "[m14_regression] $*"; }
fail() { echo "[m14_regression] FAIL: $*" >&2; exit 1; }

log "Phase 1: m14_smoke"
bash "${SCRIPT_DIR}/m14_smoke.sh"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
ADB="$(resolve_smoke_adb)"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
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
