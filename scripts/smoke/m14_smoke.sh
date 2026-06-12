#!/usr/bin/env bash
# M14 smoke: project standards alignment — source checks for docs, CI, benchmark wiring
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[m14_smoke] $*"; }
fail() { echo "[m14_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/docs/PROJECT_ALIGNMENT.md" ]] || fail "PROJECT_ALIGNMENT.md missing"
[[ -f "${ROOT}/AGENTS.md" ]] || fail "root AGENTS.md missing"
[[ -f "${ROOT}/docs/ARCHITECTURE.md" ]] || fail "ARCHITECTURE.md missing"
[[ -f "${ROOT}/CONTRIBUTING.md" ]] || fail "CONTRIBUTING.md missing"
[[ -f "${ROOT}/CODE_OF_CONDUCT.md" ]] || fail "CODE_OF_CONDUCT.md missing"
[[ -f "${ROOT}/.github/pull_request_template.md" ]] || fail "PR template missing"
[[ -f "${ROOT}/scripts/benchmark/memory_baseline.sh" ]] || fail "memory_baseline.sh missing"
[[ -f "${ROOT}/scripts/benchmark/baselines/memory_baseline.json" ]] || fail "memory baseline JSON missing"

grep -q "Gate G_RELEASE" "${ROOT}/docs/GATES.md" || fail "G_RELEASE gate missing from GATES.md"
grep -q "./gradlew lint" "${ROOT}/.github/workflows/android-ci.yml" || fail "GitHub CI missing lint step"
grep -q "matchesHistoryQuery" "${ROOT}/app/src/main/java/com/screenwakelock/detector/util/WakeEventFilters.kt" \
  || fail "WakeEventFilters.matchesHistoryQuery missing"

log "M14 source and doc checks PASS"

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
if DEVICE="$(pick_smoke_device "${ADB:-adb}" 2>/dev/null)" && [[ -n "${DEVICE}" ]]; then
  log "Device ${DEVICE} — running m13_smoke"
  bash "${SCRIPT_DIR}/m13_smoke.sh"
else
  log "No device — skipping device smoke (run m14_regression.sh with device for full pass)"
fi

log "PASS: m14_smoke"
