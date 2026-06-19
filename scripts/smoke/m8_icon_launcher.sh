#!/usr/bin/env bash
# M8 ADB: launcher icon legibility — static asset checks + screenshot crop analysis.
# Usage: bash scripts/smoke/m8_icon_launcher.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
ARTIFACT_DIR="${ARTIFACT_DIR:-${ROOT}/artifacts/m8-icon-launcher}"

log() { echo "[m8_icon_launcher] $*"; }
pass() { log "PASS: $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
[[ -n "${DEVICE}" ]] || fail "no authorized device"

ADB_S=( "${ADB}" -s "${DEVICE}" )
SAFE_SERIAL="${DEVICE//:/_}"
OUT="${ARTIFACT_DIR}/${SAFE_SERIAL}"
mkdir -p "${OUT}"

log "=== Static icon legibility ==="
bash scripts/check-icon-legibility.sh 2>&1 | tee "${OUT}/static-icon-legibility.log"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null

ui_dump() {
  "${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null | sed 's/UI hierchary dumped.*//' || true
}

find_label_bounds() {
  local pattern="$1"
  local line raw x1 y1 x2 y2
  line="$(ui_dump | tr '\n' ' ' | grep -oE "text=\"[^\"]*${pattern}[^\"]*\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 || true)"
  [[ -n "${line}" ]] || return 1
  raw="$(echo "${line}" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1)"
  raw="${raw#bounds=\"}"; raw="${raw%\"}"; raw="${raw//\]\[/,}"
  raw="${raw//[\[]/}"; raw="${raw//[\]]/}"
  IFS=',' read -r x1 y1 x2 y2 <<< "${raw}"
  [[ -n "${x1}" && -n "${y2}" ]] || return 1
  echo "${x1},${y1},${x2},${y2}"
}

wm_size() {
  "${ADB_S[@]}" shell wm size 2>/dev/null | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | head -1
}

go_home() {
  "${ADB_S[@]}" shell input keyevent KEYCODE_HOME
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_BACK 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_HOME
  sleep 2
}

open_app_drawer() {
  go_home
  local wm w h
  wm="$(wm_size)"
  w="${wm%x*}"
  h="${wm#*x}"
  w="${w:-1440}"; h="${h:-3168}"
  "${ADB_S[@]}" shell input swipe "$(( w / 2 ))" "$(( h - 80 ))" "$(( w / 2 ))" "$(( h / 2 ))" 450 2>/dev/null || true
  sleep 2
}

search_drawer() {
  local wm w
  wm="$(wm_size)"
  w="${wm%x*}"; w="${w:-1440}"
  "${ADB_S[@]}" shell input tap "$(( w / 2 ))" 320 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input text "Wakelock" 2>/dev/null || true
  sleep 2
}

analyze_shot() {
  local shot="$1"
  local tag="$2"
  local mode="$3" # bounds or auto
  local extra="${4:-}"
  local crop="${OUT}/icon_crop_${tag}.png"
  local metrics="${OUT}/icon_metrics_${tag}.json"
  local args=(python3 "${SCRIPT_DIR}/analyze_launcher_icon.py" "${shot}"
    --out-crop "${crop}" --out-json "${metrics}")
  if [[ "${mode}" == "auto" ]]; then
    args+=(--auto-color)
  else
    args+=(--bounds "${extra}")
  fi
  if "${args[@]}"; then
    pass "Icon legibility ${tag} (${metrics})"
    return 0
  fi
  return 1
}

MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
API="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
log "Device ${DEVICE} (${MODEL}, API ${API}) → ${OUT}"

log "Capture launcher home"
go_home
"${ADB_S[@]}" exec-out screencap -p > "${OUT}/01_launcher_home.png"

log "Open app drawer, search, capture"
open_app_drawer
search_drawer
"${ADB_S[@]}" exec-out screencap -p > "${OUT}/02_app_drawer_search.png"
ui_dump > "${OUT}/02_ui.xml" || true

ANALYZED=0
BOUNDS=""
for pattern in "Screen Wakelock" "Wakelock Detector" "Wakelock"; do
  BOUNDS="$(find_label_bounds "${pattern}" 2>/dev/null || true)"
  [[ -n "${BOUNDS}" ]] && break
done

if [[ -n "${BOUNDS}" ]]; then
  y1="${BOUNDS#*,}"; y1="${y1%%,*}"
  if [[ "${y1}" -lt 500 ]]; then
    log "Ignoring top search-field bounds (${BOUNDS})"
    BOUNDS=""
  else
    log "Label bounds: ${BOUNDS}"
    analyze_shot "${OUT}/02_app_drawer_search.png" "drawer_label" "bounds" "${BOUNDS}" && ANALYZED=1 || true
  fi
fi

if [[ "${ANALYZED}" -eq 0 ]]; then
  log "Brand-color auto detect on drawer search screenshot"
  analyze_shot "${OUT}/02_app_drawer_search.png" "drawer_auto" "auto" && ANALYZED=1 || true
fi

if [[ "${ANALYZED}" -eq 0 ]]; then
  log "Fallback: home workspace brand-color detect"
  analyze_shot "${OUT}/01_launcher_home.png" "home_auto" "auto" && ANALYZED=1 || true
fi

[[ "${ANALYZED}" -eq 1 ]] || fail "Icon legibility analysis failed on all screenshots"

log "Round + square mask metrics in icon_metrics_*.json"
{
  echo "# M8 launcher icon legibility"
  echo ""
  echo "- device: ${DEVICE} (${MODEL}, API ${API})"
  echo "- static: PASS (check-icon-legibility.sh)"
  echo "- detection: label bounds and/or #004D57 brand blob"
  echo "- crops: icon_crop_*.png"
  echo "- metrics: icon_metrics_*.json"
  echo "- gate: automated legibility (contrast + round/square mask survival)"
} > "${OUT}/REPORT.md"

log "DONE — ${OUT}/REPORT.md"
exit 0
