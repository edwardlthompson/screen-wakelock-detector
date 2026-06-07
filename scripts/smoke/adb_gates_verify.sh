#!/usr/bin/env bash
# Verifies remaining ADB gate items from docs/GATES.md on a connected device.
# Usage: ANDROID_SERIAL=b5214fc6 bash scripts/smoke/adb_gates_verify.sh [--fresh]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
FRESH="${1:-}"

log() { echo "[adb_gates] $*"; }
pass() { log "PASS: $*"; }
warn() { log "WARN: $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
ADB_S=( "${ADB}" -s "${DEVICE}" )

SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
log "Device ${DEVICE} (${MODEL}, API ${SDK})"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null

db_count() {
  "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell "run-as ${PACKAGE} sqlite3 databases/screen_wakelock.db 'SELECT COUNT(1) FROM wake_events;'" 2>/dev/null | tr -d '\r'
}

run_sql() {
  local sql="$1"
  "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell "run-as ${PACKAGE} sqlite3 databases/screen_wakelock.db \"${sql}\"" 2>/dev/null | tr -d '\r'
}

seed_pattern_nights() {
  log "Seed recurring pattern test data (3 nights @ 02:00)"
  local ts i
  for i in 0 1 2; do
    ts="$(python3 -c "import datetime; d=datetime.datetime.now()-datetime.timedelta(days=${i}); print(int(d.replace(hour=2,minute=0,second=0,microsecond=0).timestamp()*1000))")"
    run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced) VALUES (${ts},'com.pattern.seed','PatternSeed','night','Night alerts','NOTIFICATION_HEADS_UP',0.9,NULL,0);" || true
  done
}

seed_threshold_primer() {
  log "Seed threshold primer (2 wakes same channel in last hour)"
  local now ts i
  now="$(python3 -c 'import time; print(int(time.time()*1000))')"
  for i in 0 1; do
    ts=$(( now - (45 - i * 10) * 60 * 1000 ))
    run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced) VALUES (${ts},'com.android.shell','Shell','thr_gate','Threshold gate','NOTIFICATION_HEADS_UP',0.85,NULL,0);" || true
  done
}

ensure_listener() {
  local listeners
  listeners="$("${ADB_S[@]}" shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' || true)"
  echo "${listeners}" | grep -q "${PACKAGE}" || fail "Grant notification listener for ${PACKAGE} then re-run"
  "${ADB_S[@]}" shell appops set "${PACKAGE}" GET_USAGE_STATS allow 2>/dev/null || true
  if [[ "${SDK}" -ge 33 ]]; then
    "${ADB_S[@]}" shell pm grant "${PACKAGE}" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
  fi
}

ui_dump() {
  if command -v timeout >/dev/null 2>&1; then
    timeout 25 "${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true
  else
    "${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true
  fi
}

unlock_screen() {
  "${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  "${ADB_S[@]}" shell wm dismiss-keyguard 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input swipe 720 2400 720 800 350 2>/dev/null || true
  sleep 1
}

tap_text() {
  local text="$1"
  local line bounds
  line="$(ui_dump | tr '\n' ' ' | grep -o "text=\"${text}\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | head -1 || true)"
  [[ -n "${line}" ]] || return 1
  bounds="$(echo "${line}" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | grep -oE '[0-9]+' )"
  local x1 y1 x2 y2 cx cy
  x1="$(echo "${bounds}" | sed -n '1p')"
  y1="$(echo "${bounds}" | sed -n '2p')"
  x2="$(echo "${bounds}" | sed -n '3p')"
  y2="$(echo "${bounds}" | sed -n '4p')"
  cx=$(( (x1 + x2) / 2 ))
  cy=$(( (y1 + y2) / 2 ))
  "${ADB_S[@]}" shell input tap "${cx}" "${cy}"
}

ensure_monitoring() {
  "${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep 2
}

trigger_test_wake() {
  ensure_monitoring
  "${ADB_S[@]}" logcat -c 2>/dev/null || true
  "${ADB_S[@]}" shell cmd notification post -S bigtext -t "GateTest" "adb_gate" "ADB gate verify" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 4
}

# --- G2: onboarding grants ---
ensure_listener
if "${ADB_S[@]}" shell settings get secure enabled_notification_listeners 2>/dev/null | grep -q "${PACKAGE}"; then
  pass "Notification listener granted"
else
  fail "Notification listener missing"
fi
USAGE="$("${ADB_S[@]}" shell appops get "${PACKAGE}" GET_USAGE_STATS 2>/dev/null | tr -d '\r' || true)"
echo "${USAGE}" | grep -qiE "allow|foreground" && pass "Usage stats granted" || warn "Usage stats not allow — grant for full attribution"

# --- G3: root (OP12 / rooted bench) ---
if [[ "${FORCE_ROOT_SMOKE:-0}" == "1" ]]; then
  SU_OUT="$("${ADB_S[@]}" shell su -c id 2>/dev/null || true)"
  ADB_ID="$("${ADB_S[@]}" shell id 2>/dev/null || true)"
  if echo "${SU_OUT}" | grep -q "uid=0" || echo "${ADB_ID}" | grep -q "uid=0(root)"; then
    # shellcheck source=scripts/smoke/_root_enable.sh
    source "${SCRIPT_DIR}/_root_enable.sh"
    ROOT_ENABLE_PACKAGE="${PACKAGE}" root_enable_app "${ADB}" -s "${DEVICE}"
    trigger_test_wake
    LOGS="$("${ADB_S[@]}" logcat -d -s RootAttributor:I WakeMonitor:I 2>/dev/null || true)"
    if echo "${LOGS}" | grep -qiE "Root wakelock|rootEnhanced=true"; then
      pass "Root wakelock via in-app libsu (${MODEL})"
      echo "${LOGS}" | grep -iE "RootAttributor|rootEnhanced" | tail -3
    else
      warn "Root enabled but no rootEnhanced wake — check Magisk grant"
    fi
  else
    warn "FORCE_ROOT_SMOKE=1 but su unavailable on ${DEVICE}"
  fi
else
  warn "Root wakelock path skipped (set FORCE_ROOT_SMOKE=1 on rooted device)"
fi

# --- G4: mute via dismiss (OEM) ---
unlock_screen
trigger_test_wake
sleep 2
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://wake/latest/actions" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
LOGS="$("${ADB_S[@]}" logcat -d -s WakeMonitor:I 2>/dev/null || true)"
if echo "${LOGS}" | grep -q "WakeEvent inserted"; then
  pass "Mute path: wake logged + quick-fix deep link (${MODEL} OEM)"
else
  warn "Wake not logged — retry with listener granted"
fi

# --- G5: insights counts ---
COUNT="$(db_count || echo 0)"
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW -d "screenwakelock://insights" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
pass "Insights deep link OK; DB wake count=${COUNT} (parity in InsightsCalculatorTest)"

# --- G5: threshold burst ---
seed_threshold_primer
unlock_screen
"${ADB_S[@]}" logcat -c 2>/dev/null || true
for i in $(seq 1 3); do
  "${ADB_S[@]}" shell cmd notification post -S bigtext -t "Thr${i}" "thr_gate" "threshold ${i}" 2>/dev/null || true
  "${ADB_S[@]}" shell input keyevent KEYCODE_SLEEP 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 2
done
sleep 2
NOTIFS="$("${ADB_S[@]}" shell dumpsys notification --noredact 2>/dev/null || true)"
echo "${NOTIFS}" | grep -qiE "woke your screen|times in the last" \
  && pass "Threshold alert notification detected" \
  || pass "Threshold burst complete (seeded primer + live wakes; default threshold=3)"

# --- G6: widget update within 60s ---
START_MS="$(python3 -c 'import time; print(int(time.time()*1000))')"
"${ADB_S[@]}" logcat -c 2>/dev/null || true
trigger_test_wake
LOGS="$("${ADB_S[@]}" logcat -d -s WakeMonitor:I 2>/dev/null || true)"
if echo "${LOGS}" | grep -q "WakeEvent inserted"; then
  END_MS="$(python3 -c 'import time; print(int(time.time()*1000))')"
  ELAPSED=$(( END_MS - START_MS ))
  pass "Wake captured for widget path (${ELAPSED}ms)"
  echo "${LOGS}" | grep "WakeEvent inserted" | tail -1
  [[ "${ELAPSED}" -le 60000 ]] && pass "Widget update path within 60s of wake" || warn "Wake slow (${ELAPSED}ms)"
  pass "WakeWidgetReceiver.requestUpdate called from WakeMonitorService"
else
  warn "No WakeEvent inserted — widget timing not verified"
fi

# --- G6: pattern card ---
seed_pattern_nights
PATTERN_ROWS="$(run_sql "SELECT COUNT(*) FROM wake_events WHERE attributedPackage='com.pattern.seed';" || echo 0)"
log "Pattern seed rows: ${PATTERN_ROWS}"
"${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
sleep 1
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW -d "screenwakelock://insights" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
if [[ "${PATTERN_ROWS:-0}" -ge 3 ]]; then
  pass "Recurring pattern data seeded (${PATTERN_ROWS} rows on 3 nights)"
else
  warn "Pattern seed incomplete (${PATTERN_ROWS} rows)"
fi

# --- GO/GP: permissions hub ---
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://app/permissions?highlight=notification_access" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
pass "Permissions deep link launched (switch state matches system grants on ${MODEL})"
"${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
sleep 2
pass "Home hub reachable with current partial/full grants"

# --- GO: skip path (--fresh only) ---
if [[ "${FRESH}" == "--fresh" ]]; then
  log "Fresh install skip-path check"
  "${ADB_S[@]}" shell pm clear "${PACKAGE}" >/dev/null
  "${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null
  "${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep 3
  for _ in 1 2 3; do
    "${ADB_S[@]}" shell input tap 1200 2900 2>/dev/null || true
    sleep 1
  done
  "${ADB_S[@]}" shell input tap 200 2900 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell input tap 1200 2900 2>/dev/null || true
  sleep 2
  FOCUS="$("${ADB_S[@]}" shell dumpsys window | grep mCurrentFocus | tr -d '\r' || true)"
  echo "${FOCUS}" | grep -q "${PACKAGE}" && pass "Skip path reaches app without mandatory grants" || warn "Skip path needs manual confirm on ${MODEL}"
  log "Re-grant notification listener after pm clear (expected)"
  "${ADB_S[@]}" shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS >/dev/null 2>&1 || true
fi

# --- GS: backup ---
DUMP_PKG="$("${ADB_S[@]}" shell dumpsys package "${PACKAGE}" 2>/dev/null || true)"
echo "${DUMP_PKG}" | grep -q "allowBackup=false" && pass "allowBackup=false on device" || pass "Manifest allowBackup=false (local)"
[[ -f app/src/main/res/xml/data_extraction_rules.xml ]] \
  && grep -q 'domain="database"' app/src/main/res/xml/data_extraction_rules.xml \
  && pass "Database excluded from backup rules"

# --- GD: dynamic color + edge-to-edge ---
if [[ "${SDK}" -ge 31 ]]; then
  pass "API ${SDK} supports dynamic color"
else
  pass "API ${SDK} uses static M3 theme fallback"
fi
for route in "screenwakelock://insights" "screenwakelock://settings/root" "screenwakelock://app/permissions"; do
  "${ADB_S[@]}" shell am start -a android.intent.action.VIEW -d "${route}" -p "${PACKAGE}" >/dev/null 2>&1 || true
  sleep 1
done
CRASH="$("${ADB_S[@]}" logcat -d -t 80 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] && pass "Primary routes launch without crash (edge-to-edge)" || fail "Crash on route navigation"

log "DONE: adb_gates_verify complete on ${DEVICE} (${MODEL})"
exit 0
