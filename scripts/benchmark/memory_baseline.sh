#!/usr/bin/env bash
# Memory baseline benchmark — parses adb dumpsys meminfo and compares to baseline JSON.
# Usage: scripts/benchmark/memory_baseline.sh [--update-baseline]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
ADB="${ADB:-adb}"
BASELINE_FILE="${BASELINE_FILE:-${SCRIPT_DIR}/baselines/memory_baseline.json}"
HISTORY_DIR="${HISTORY_DIR:-${ROOT}/artifacts/benchmark-history}"
ITERATIONS="${ITERATIONS:-5}"
UPDATE_BASELINE="${UPDATE_BASELINE:-0}"

if [[ "${1:-}" == "--update-baseline" ]]; then
  UPDATE_BASELINE=1
fi

log() { echo "[memory_baseline] $*"; }
fail() { echo "[memory_baseline] FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${ROOT}/scripts/smoke/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "no authorized device — set SMOKE_DEVICE"
ADB_S=( "${ADB}" -s "${DEVICE}" )

[[ -f "${BASELINE_FILE}" ]] || fail "baseline missing: ${BASELINE_FILE}"

MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
VERSION="$("${ADB_S[@]}" shell dumpsys package "${PACKAGE}" 2>/dev/null | grep versionName | head -1 | sed 's/.*=//' | tr -d '\r' || echo unknown)"
log "Device ${DEVICE} (${MODEL}, API ${SDK}) app ${VERSION}"

if ! "${ADB_S[@]}" shell pm path "${PACKAGE}" >/dev/null 2>&1; then
  fail "app not installed — run ./gradlew assembleDebug && adb install"
fi

log "Launch app and ensure monitoring foreground"
"${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
sleep 2

parse_meminfo() {
  local raw pss java_heap
  raw="$("${ADB_S[@]}" shell dumpsys meminfo "${PACKAGE}" 2>/dev/null || true)"
  pss="$(echo "${raw}" | awk '/^TOTAL PSS:/ {print $3; exit}')"
  if [[ -z "${pss}" ]]; then
    pss="$(echo "${raw}" | awk '/TOTAL/ && /PSS/ {for(i=1;i<=NF;i++) if($i ~ /^[0-9]+$/) {print $i; exit}}')"
  fi
  java_heap="$(echo "${raw}" | awk '/Java Heap:/ {print $3; exit}')"
  [[ -n "${pss}" ]] || fail "could not parse TOTAL PSS from dumpsys meminfo"
  [[ -n "${java_heap}" ]] || java_heap=0
  echo "${pss} ${java_heap}"
}

log "Warm-up + ${ITERATIONS} meminfo samples"
for _ in $(seq 1 3); do
  parse_meminfo >/dev/null
  sleep 1
done

last_pss=0
last_java=0
for i in $(seq 1 "${ITERATIONS}"); do
  read -r last_pss last_java <<< "$(parse_meminfo)"
  log "Sample ${i}/${ITERATIONS}: PSS=${last_pss}kB JavaHeap=${last_java}kB"
  sleep 1
done

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
mkdir -p "${HISTORY_DIR}"
RUN_FILE="${HISTORY_DIR}/memory_${TIMESTAMP//:/-}_${DEVICE}.json"

python3 - "${RUN_FILE}" "${VERSION}" "${DEVICE}" "${MODEL}" "${TIMESTAMP}" "${last_pss}" "${last_java}" "${ITERATIONS}" <<'PY'
import json, sys
path, version, device, model, ts, pss, java_heap, iters = sys.argv[1:9]
data = {
    "version": version,
    "device": device,
    "device_model": model,
    "timestamp": ts,
    "pss_kb": int(pss),
    "java_heap_kb": int(java_heap),
    "iterations": int(iters),
}
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
print(json.dumps(data))
PY

log "Wrote ${RUN_FILE}"

if [[ "${UPDATE_BASELINE}" == "1" ]]; then
  python3 - "${BASELINE_FILE}" "${last_pss}" "${last_java}" <<'PY'
import json, sys
path, pss, java_heap = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
with open(path, encoding="utf-8") as f:
    base = json.load(f)
base["pss_kb"] = pss
base["java_heap_kb"] = java_heap
with open(path, "w", encoding="utf-8") as f:
    json.dump(base, f, indent=2)
print(f"Updated baseline: PSS={pss}kB JavaHeap={java_heap}kB")
PY
  log "Baseline updated at ${BASELINE_FILE}"
  exit 0
fi

python3 - "${BASELINE_FILE}" "${RUN_FILE}" "${HISTORY_DIR}" <<'PY'
import json, sys, glob, os

baseline_path, run_path, history_dir = sys.argv[1:4]
with open(baseline_path, encoding="utf-8") as f:
    base = json.load(f)
with open(run_path, encoding="utf-8") as f:
    run = json.load(f)

pss_max = base["pss_kb"] * (1 + base.get("delta_percent_max", 8) / 100)
pss_max = max(pss_max, base["pss_kb"] + base.get("delta_mb_max", 15) * 1024)
java_max = base["java_heap_kb"] * (1 + base.get("delta_percent_max", 8) / 100)
java_max = max(java_max, base["java_heap_kb"] + base.get("delta_mb_max", 15) * 1024)

errors = []
if run["pss_kb"] > pss_max:
    errors.append(f"PSS {run['pss_kb']}kB exceeds threshold {int(pss_max)}kB (baseline {base['pss_kb']}kB)")
if run["java_heap_kb"] > java_max:
    errors.append(f"Java heap {run['java_heap_kb']}kB exceeds threshold {int(java_max)}kB (baseline {base['java_heap_kb']}kB)")

window = base.get("monotonic_window", 3)
runs = []
for p in sorted(glob.glob(os.path.join(history_dir, "memory_*.json"))):
    try:
        with open(p, encoding="utf-8") as f:
            runs.append(json.load(f))
    except (json.JSONDecodeError, OSError):
        pass
runs.sort(key=lambda r: r.get("timestamp", ""))
recent = [r["pss_kb"] for r in runs[-window:] if "pss_kb" in r]
if len(recent) >= window and all(recent[i] < recent[i + 1] for i in range(len(recent) - 1)):
    errors.append(f"PSS increased monotonically over last {window} runs: {recent}")

if errors:
    for e in errors:
        print(f"FAIL: {e}", file=sys.stderr)
    sys.exit(1)
print(f"PASS: PSS={run['pss_kb']}kB JavaHeap={run['java_heap_kb']}kB within baseline thresholds")
PY

log "PASS: memory baseline check"
