#!/usr/bin/env bash
# Memory baseline benchmark — parses adb dumpsys meminfo and compares to device baseline JSON.
# Usage: scripts/benchmark/memory_baseline.sh [--update-baseline]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
ADB="${ADB:-adb}"
FALLBACK_BASELINE="${SCRIPT_DIR}/baselines/memory_baseline.json"
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

[[ -f "${FALLBACK_BASELINE}" ]] || fail "fallback baseline missing: ${FALLBACK_BASELINE}"

MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r' | tr -cd '[:alnum:]._-' )"
SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
VERSION="$("${ADB_S[@]}" shell dumpsys package "${PACKAGE}" 2>/dev/null | grep versionName | head -1 | sed 's/.*=//' | tr -d '\r' || echo unknown)"

DEVICE_BASELINE_DIR="${SCRIPT_DIR}/baselines/devices"
mkdir -p "${DEVICE_BASELINE_DIR}"
DEVICE_BASELINE="${DEVICE_BASELINE_DIR}/${MODEL}.json"
BASELINE_FILE="${DEVICE_BASELINE}"
SEED_BASELINE=0

if [[ ! -f "${DEVICE_BASELINE}" ]]; then
  BASELINE_FILE="${FALLBACK_BASELINE}"
  SEED_BASELINE=1
fi

log "Device ${DEVICE} (${MODEL}, API ${SDK}) app ${VERSION}"
log "Baseline file: ${BASELINE_FILE}"

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

write_baseline() {
  local target="$1"
  python3 - "${target}" "${MODEL}" "${last_pss}" "${last_java}" <<'PY'
import json, sys
path, model, pss, java_heap = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
try:
    with open(path, encoding="utf-8") as f:
        base = json.load(f)
except FileNotFoundError:
    base = {
        "version": "1.0",
        "device_model": model,
        "description": f"Device baseline for {model}",
        "delta_percent_max": 8,
        "delta_mb_max": 15,
        "monotonic_window": 3,
    }
base["device_model"] = model
base["pss_kb"] = pss
base["java_heap_kb"] = java_heap
with open(path, "w", encoding="utf-8") as f:
    json.dump(base, f, indent=2)
print(f"Updated baseline: PSS={pss}kB JavaHeap={java_heap}kB")
PY
}

if [[ "${UPDATE_BASELINE}" == "1" ]]; then
  write_baseline "${DEVICE_BASELINE}"
  log "Device baseline updated at ${DEVICE_BASELINE}"
  exit 0
fi

if [[ "${SEED_BASELINE}" == "1" ]]; then
  write_baseline "${DEVICE_BASELINE}"
  log "Seeded new device baseline at ${DEVICE_BASELINE}"
  log "hint: re-run with --update-baseline after intentional memory changes"
  log "PASS: first capture on ${MODEL} — baseline seeded"
  exit 0
fi

python3 - "${BASELINE_FILE}" "${RUN_FILE}" "${HISTORY_DIR}" "${DEVICE}" "${MODEL}" <<'PY'
import json, sys, glob, os

baseline_path, run_path, history_dir, device_serial, device_model = sys.argv[1:6]
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
            row = json.load(f)
    except (json.JSONDecodeError, OSError):
        continue
    if row.get("device") == device_serial or row.get("device_model") == device_model:
        runs.append(row)
runs.sort(key=lambda r: r.get("timestamp", ""))
recent = [r["pss_kb"] for r in runs[-window:] if "pss_kb" in r]
if len(recent) >= window and all(recent[i] < recent[i + 1] for i in range(len(recent) - 1)):
    errors.append(f"PSS increased monotonically over last {window} runs on {device_model}: {recent}")

if errors:
    for e in errors:
        print(f"FAIL: {e}", file=sys.stderr)
    sys.exit(1)
print(f"PASS: PSS={run['pss_kb']}kB JavaHeap={run['java_heap_kb']}kB within baseline thresholds")
PY

log "PASS: memory baseline check"
