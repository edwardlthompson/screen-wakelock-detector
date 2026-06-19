#!/usr/bin/env bash
# Attribution pipeline verification: permissions, fresh + stale notification wakes,
# active-notification snapshot path, DB unknown ratio, optional root (OP12).
#
# Usage:
#   SMOKE_DEVICE=8bf09993 bash scripts/smoke/attribution_verify.sh
#   SMOKE_DEVICE=192.168.1.2:44487 FORCE_ROOT_SMOKE=1 bash scripts/smoke/attribution_verify.sh
#   bash scripts/smoke/attribution_dual_verify.sh   # both devices when online
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
NLS="${PACKAGE}/com.screenwakelock.detector.service.NotificationCaptureService"
FRESH_CHANNEL="${FRESH_CHANNEL:-attr_fresh}"
STALE_CHANNEL="${STALE_CHANNEL:-attr_stale}"
STALE_WAIT_SEC="${STALE_WAIT_SEC:-12}"
DB_PATH="/data/data/${PACKAGE}/databases/screen_wakelock.db"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

log() { echo "[attr_verify] $*"; }
pass() { PASS_COUNT=$((PASS_COUNT + 1)); log "PASS: $*"; }
warn() { WARN_COUNT=$((WARN_COUNT + 1)); log "WARN: $*"; }
fail() { FAIL_COUNT=$((FAIL_COUNT + 1)); log "FAIL: $*" >&2; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
ADB="$(resolve_smoke_adb)"
DEVICE="$(pick_smoke_device "${ADB}")" || { fail "device selection failed"; exit 1; }
[[ -n "${DEVICE}" ]] || { fail "no authorized device"; exit 1; }

ADB_S=( "${ADB}" -s "${DEVICE}" )
MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
SERIAL_HW="$("${ADB_S[@]}" shell getprop ro.serialno | tr -d '\r')"
log "Device ${DEVICE} (${MODEL}, API ${SDK}, hw=${SERIAL_HW})"

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug

if ! "${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null 2>&1; then
  log "Install failed — uninstalling prior build"
  "${ADB_S[@]}" uninstall "${PACKAGE}" || true
  "${ADB_S[@]}" install "${APK_PATH}"
fi

VERSION="$("${ADB_S[@]}" shell dumpsys package "${PACKAGE}" | grep versionName | head -1 | tr -d '\r' || true)"
log "Installed ${VERSION}"

ensure_listener() {
  local listeners
  listeners="$("${ADB_S[@]}" shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
  if echo "${listeners}" | grep -q "${PACKAGE}"; then
    return 0
  fi
  log "Attempting adb allow_listener"
  "${ADB_S[@]}" shell cmd notification allow_listener "${NLS}" 2>/dev/null || true
  listeners="$("${ADB_S[@]}" shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
  echo "${listeners}" | grep -q "${PACKAGE}"
}

ensure_usage() {
  "${ADB_S[@]}" shell appops set "${PACKAGE}" GET_USAGE_STATS allow 2>/dev/null || true
  local usage
  usage="$("${ADB_S[@]}" shell appops get "${PACKAGE}" GET_USAGE_STATS 2>/dev/null | tr -d '\r' || true)"
  echo "${usage}" | grep -qiE "allow|foreground"
}

ensure_monitoring() {
  "${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep 2
}

unlock_screen() {
  "${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  "${ADB_S[@]}" shell wm dismiss-keyguard 2>/dev/null || true
  sleep 1
}

screen_cycle() {
  "${ADB_S[@]}" shell input keyevent KEYCODE_HOME 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_SLEEP 2>/dev/null \
    || "${ADB_S[@]}" shell input keyevent KEYCODE_POWER 2>/dev/null || true
  sleep 2
  "${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null \
    || "${ADB_S[@]}" shell input keyevent KEYCODE_POWER 2>/dev/null || true
  sleep 4
}

run_sql() {
  local sql="$1"
  local out
  # Prefer adb root (OP13 USB root); fall back to run-as.
  if "${ADB_S[@]}" shell id 2>/dev/null | grep -q "uid=0(root)"; then
    out="$("${ADB_S[@]}" shell "sqlite3 '${DB_PATH}' \"${sql}\"" 2>/dev/null | tr -d '\r' || true)"
  else
    "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
    sleep 1
    out="$("${ADB_S[@]}" shell "run-as ${PACKAGE} sqlite3 databases/screen_wakelock.db \"${sql}\"" 2>/dev/null | tr -d '\r' || true)"
  fi
  echo "${out}"
}

count_wakes_since() {
  local since_ms="$1"
  run_sql "SELECT COUNT(*) FROM wake_events WHERE timestampMillis >= ${since_ms};"
}

count_unknown_since() {
  local since_ms="$1"
  run_sql "SELECT COUNT(*) FROM wake_events WHERE timestampMillis >= ${since_ms} AND (attributedPackage IS NULL OR reasonCode='UNKNOWN');"
}

latest_attributed_wake() {
  local since_ms="$1"
  run_sql "SELECT id,attributedPackage,reasonCode,confidence FROM wake_events WHERE timestampMillis >= ${since_ms} ORDER BY timestampMillis DESC LIMIT 1;"
}

wait_for_wake() {
  local since_ms="$1"
  local attempts="${2:-8}"
  local i row
  for i in $(seq 1 "${attempts}"); do
    row="$(latest_attributed_wake "${since_ms}")"
    [[ -n "${row}" ]] && { echo "${row}"; return 0; }
    sleep 1
  done
  return 1
}

grep_attribution_logs() {
  local since_tag="$1"
  "${ADB_S[@]}" logcat -d -s WakeAttributor:I WakeMonitor:I NotificationCapture:W 2>/dev/null \
    | tr -d '\r' || true
}

trigger_fresh_wake() {
  local channel="$1"
  local title="$2"
  ensure_monitoring
  "${ADB_S[@]}" logcat -c 2>/dev/null || true
  "${ADB_S[@]}" shell cmd notification post -S bigtext -t "${title}" "${channel}" "attribution verify" 2>/dev/null || true
  sleep 1
  if [[ -n "$(notification_posted "${title}")" ]]; then
    pass "Test notification visible in dumpsys (${title})"
  else
    warn "Test notification not found in dumpsys — listener may miss shell posts on ${MODEL}"
  fi
  unlock_screen
  screen_cycle
}

trigger_stale_wake() {
  local channel="$1"
  local title="$2"
  ensure_monitoring
  "${ADB_S[@]}" logcat -c 2>/dev/null || true
  "${ADB_S[@]}" shell cmd notification post -S bigtext -t "${title}" "${channel}" "stale attribution verify" 2>/dev/null || true
  log "Waiting ${STALE_WAIT_SEC}s (outside 5000ms correlation window) with notification still posted"
  sleep "${STALE_WAIT_SEC}"
  unlock_screen
  screen_cycle
}

probe_root() {
  local su_out adb_id
  su_out="$("${ADB_S[@]}" shell su -c id 2>/dev/null || true)"
  adb_id="$("${ADB_S[@]}" shell id 2>/dev/null || true)"
  if echo "${su_out}" | grep -q "uid=0" || echo "${adb_id}" | grep -q "uid=0(root)"; then
    return 0
  fi
  return 1
}

# --- Phase 0: permissions ---
log "Phase 0: permission probes"
if ensure_listener; then
  pass "Notification listener enabled (${NLS})"
else
  fail "Notification listener not enabled — grant in Settings → Notification access"
fi

if ensure_usage; then
  pass "Usage stats allowed"
else
  warn "Usage stats not allow — fallback attribution limited"
fi

LISTENER_DISCONNECT="$("${ADB_S[@]}" logcat -d -s NotificationCapture:W 2>/dev/null | grep -i disconnected || true)"
if [[ -n "${LISTENER_DISCONNECT}" ]]; then
  warn "NotificationCapture disconnect seen in logcat — restart app or re-bind listener"
else
  pass "No NotificationCapture disconnect warnings in recent logcat"
fi

# Baseline unknown ratio (last 24h)
BASELINE_SINCE=$(( $(date +%s) * 1000 - 86400000 ))
BASELINE_TOTAL="$(run_sql "SELECT COUNT(*) FROM wake_events WHERE timestampMillis >= ${BASELINE_SINCE};")"
BASELINE_UNKNOWN="$(run_sql "SELECT COUNT(*) FROM wake_events WHERE timestampMillis >= ${BASELINE_SINCE} AND (attributedPackage IS NULL OR reasonCode='UNKNOWN');")"
log "Baseline 24h: total=${BASELINE_TOTAL:-0} unknown=${BASELINE_UNKNOWN:-0}"
if [[ -n "${BASELINE_TOTAL}" && "${BASELINE_TOTAL}" != "0" ]]; then
  pass "Historical wake data present (${BASELINE_TOTAL} rows / 24h)"
  REASON_BREAKDOWN="$(run_sql "SELECT reasonCode || '|' || COUNT(1) FROM wake_events WHERE timestampMillis >= ${BASELINE_SINCE} GROUP BY reasonCode ORDER BY COUNT(1) DESC;")"
  log "Reason breakdown (24h):"
  echo "${REASON_BREAKDOWN}" | while IFS= read -r line; do
    [[ -n "${line}" ]] && log "  ${line}"
  done
  if [[ -n "${BASELINE_UNKNOWN}" && "${BASELINE_UNKNOWN}" != "0" && -n "${BASELINE_TOTAL}" ]]; then
    UNKNOWN_PCT=$(( BASELINE_UNKNOWN * 100 / BASELINE_TOTAL ))
    log "Unknown rate (24h): ${UNKNOWN_PCT}% (${BASELINE_UNKNOWN}/${BASELINE_TOTAL})"
    if [[ "${UNKNOWN_PCT}" -gt 40 ]]; then
      warn "High unknown rate on ${MODEL} — verify listener health + active notifications at wake"
    fi
  fi
else
  warn "No wakes in last 24h before test — device may be fresh"
fi

notification_posted() {
  local title="$1"
  "${ADB_S[@]}" shell dumpsys notification --noredact 2>/dev/null \
    | grep -F "${title}" | head -1 || true
}

# --- Phase 1: fresh notification wake ---
log "Phase 1: fresh notification → screen wake"
TEST_START=$(( $(date +%s) * 1000 - 2000 ))
trigger_fresh_wake "${FRESH_CHANNEL}" "AttrFresh"
LOGS="$(grep_attribution_logs fresh)"
if echo "${LOGS}" | grep -q "WakeEvent inserted"; then
  pass "WakeEvent inserted after fresh notification wake"
else
  fail "No WakeEvent inserted log after fresh wake"
fi

if echo "${LOGS}" | grep "Attributed wake" | grep -qv "reason=UNKNOWN"; then
  pass "Fresh wake attributed (not UNKNOWN)"
  echo "${LOGS}" | grep "Attributed wake" | tail -1
else
  fail "Fresh wake still UNKNOWN — check listener + monitoring service"
fi

ROW="$(wait_for_wake "${TEST_START}" 10 || true)"
if [[ -n "${ROW}" ]]; then
  pass "DB row persisted: ${ROW}"
  if echo "${ROW}" | grep -qE "NOTIFICATION_|com\.android\.shell|com\.screenwakelock"; then
    pass "DB reason/package looks notification-attributed"
  else
    warn "DB row present but package/reason unexpected: ${ROW}"
  fi
else
  fail "No wake_events row in DB after fresh test"
fi

# --- Phase 2: stale ongoing notification (active snapshot) ---
log "Phase 2: stale posted notification → screen wake (active snapshot path)"
STALE_START=$(( $(date +%s) * 1000 - 2000 ))
trigger_stale_wake "${STALE_CHANNEL}" "AttrStale"
STALE_LOGS="$(grep_attribution_logs stale)"
STALE_ROW="$(wait_for_wake "${STALE_START}" 10 || true)"

if echo "${STALE_LOGS}" | grep "Attributed wake" | tail -1 | grep -qv "reason=UNKNOWN"; then
  pass "Stale wake attributed via log (active snapshot or usage fallback)"
  echo "${STALE_LOGS}" | grep "Attributed wake" | tail -1
elif [[ -n "${STALE_ROW}" ]] && echo "${STALE_ROW}" | grep -qv "UNKNOWN"; then
  pass "Stale wake attributed in DB: ${STALE_ROW}"
else
  # Shell test notifications may be low importance — warn not fail
  warn "Stale wake not attributed (shell channel may be below HIGH importance — manual alarm/call test advised)"
  echo "${STALE_LOGS}" | grep "Attributed wake" | tail -1 || true
fi

# --- Phase 3: session unknown ratio ---
log "Phase 3: session unknown ratio"
SESSION_TOTAL="$(count_wakes_since "${TEST_START}")"
SESSION_UNKNOWN="$(count_unknown_since "${TEST_START}")"
log "Session wakes: total=${SESSION_TOTAL:-0} unknown=${SESSION_UNKNOWN:-0}"
if [[ "${SESSION_UNKNOWN:-0}" == "0" && "${SESSION_TOTAL:-0}" -ge 1 ]]; then
  pass "All session test wakes attributed (0 unknown)"
elif [[ "${SESSION_TOTAL:-0}" -ge 1 ]]; then
  ATTRIB=$(( SESSION_TOTAL - SESSION_UNKNOWN ))
  warn "Session attribution ${ATTRIB}/${SESSION_TOTAL} — some manual wakes expected on ${MODEL}"
else
  fail "No session wakes recorded"
fi

# --- Phase 4: optional root (OP12 / Magisk) ---
if [[ "${FORCE_ROOT_SMOKE:-0}" == "1" ]]; then
  log "Phase 4: root attribution (FORCE_ROOT_SMOKE=1)"
  if probe_root; then
    pass "Root/su available on ${DEVICE}"
    # shellcheck source=scripts/smoke/_root_enable.sh
    source "${SCRIPT_DIR}/_root_enable.sh"
    ROOT_ENABLE_PACKAGE="${PACKAGE}" root_enable_app "${ADB}" -s "${DEVICE}"
    ROOT_START=$(( $(date +%s) * 1000 - 2000 ))
    trigger_fresh_wake "root_attr" "RootAttr"
    ROOT_LOGS="$("${ADB_S[@]}" logcat -d -s RootAttributor:I RootAttributor:D WakeMonitor:I 2>/dev/null | tr -d '\r' || true)"
    if echo "${ROOT_LOGS}" | grep -qiE "Root wakelock|rootEnhanced=true|Root wakelock from"; then
      pass "Root-enhanced attribution logged"
      echo "${ROOT_LOGS}" | grep -iE "RootAttributor|rootEnhanced" | tail -3
    else
      warn "Root available but no rootEnhanced wake — grant Magisk su to ${PACKAGE}"
    fi
    ROOT_ROW="$(run_sql "SELECT rootEnhanced,reasonCode,wakelockTag FROM wake_events WHERE timestampMillis >= ${ROOT_START} ORDER BY timestampMillis DESC LIMIT 1;")"
    [[ -n "${ROOT_ROW}" ]] && log "Latest root wake row: ${ROOT_ROW}"
  else
    warn "FORCE_ROOT_SMOKE=1 but su/adb root unavailable on ${DEVICE}"
  fi
else
  log "Phase 4: root skipped (set FORCE_ROOT_SMOKE=1 on rooted OP12)"
fi

# --- Summary ---
CRASH="$("${ADB_S[@]}" logcat -d -t 80 2>/dev/null | grep -E "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash detected during verification"

log "Summary ${DEVICE}: PASS=${PASS_COUNT} WARN=${WARN_COUNT} FAIL=${FAIL_COUNT}"

ARTIFACT_DIR="${ROOT}/artifacts/attr-verify/${DEVICE//:/_}"
mkdir -p "${ARTIFACT_DIR}"
{
  echo "# Attribution verify — $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo ""
  echo "- device: ${DEVICE} (${MODEL}, API ${SDK}, hw=${SERIAL_HW})"
  echo "- version: ${VERSION}"
  echo "- pass: ${PASS_COUNT} warn: ${WARN_COUNT} fail: ${FAIL_COUNT}"
  echo "- baseline 24h: total=${BASELINE_TOTAL:-0} unknown=${BASELINE_UNKNOWN:-0}"
  echo "- session: total=${SESSION_TOTAL:-0} unknown=${SESSION_UNKNOWN:-0}"
  echo ""
  echo "## Reason breakdown (24h)"
  echo '```'
  echo "${REASON_BREAKDOWN:-n/a}"
  echo '```'
} > "${ARTIFACT_DIR}/REPORT.md"
log "Report: ${ARTIFACT_DIR}/REPORT.md"

if [[ "${FAIL_COUNT}" -gt 0 ]]; then
  log "Record: Smoke attr_verify: FAIL $(date -u +%Y-%m-%dT%H:%M:%SZ) ${DEVICE} ${VERSION}"
  exit 1
fi

log "Record: Smoke attr_verify: PASS $(date -u +%Y-%m-%dT%H:%M:%SZ) ${DEVICE} ${VERSION}"
exit 0
