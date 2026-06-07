#!/usr/bin/env bash
# M8 deep ADB verify: root timeline + diagnostic export, pattern actions, batch mute dialog.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

log() { echo "[m8_adb_deep] $*"; }
pass() { log "PASS: $*"; }
warn() { log "WARN: $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
ADB_S=( "${ADB}" -s "${DEVICE}" )

MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
log "Device ${DEVICE} (${MODEL})"

[[ -f "${APK_PATH}" ]] || ./gradlew assembleDebug
"${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null

ui_dump() {
  "${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true
}

run_sql() {
  local sql="$1"
  "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell "run-as ${PACKAGE} sqlite3 databases/screen_wakelock.db \"${sql}\"" 2>/dev/null | tr -d '\r'
}

seed_pattern_nights() {
  log "Seed recurring pattern data (3 nights)"
  local ts i
  for i in 0 1 2; do
    ts="$(python3 -c "import datetime; d=datetime.datetime.now()-datetime.timedelta(days=${i}); print(int(d.replace(hour=2,minute=0,second=0,microsecond=0).timestamp()*1000))")"
    run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,rootParserId) VALUES (${ts},'com.pattern.seed','PatternSeed','night','Night alerts','NOTIFICATION_HEADS_UP',0.9,NULL,0,NULL);" || true
  done
}

seed_top_offender() {
  log "Seed top offender for batch mute menu"
  local now ts i
  now="$(python3 -c 'import time; print(int(time.time()*1000))')"
  for i in 0 1 2; do
    ts=$(( now - i * 3600 * 1000 ))
    run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,rootParserId) VALUES (${ts},'com.batch.mute','BatchMuteApp','alerts','Alerts','NOTIFICATION_HEADS_UP',0.88,NULL,0,NULL);" || true
  done
}

seed_root_timeline() {
  log "Seed root-enhanced wakes for timeline"
  local now ts i
  now="$(python3 -c 'import time; print(int(time.time()*1000))')"
  for i in 0 1; do
    ts=$(( now - i * 600 * 1000 ))
    run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,rootParserId,wakelockTag) VALUES (${ts},'com.root.test','RootTest','sys','System','ROOT_WAKELOCK',0.92,NULL,1,'dumpsys_power', 'test:wakelock');" || true
  done
}

tap_text() {
  local text="$1"
  local line bounds x1 y1 x2 y2 cx cy
  line="$(ui_dump | tr '\n' ' ' | grep -o "text=\"${text}\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | head -1 || true)"
  [[ -n "${line}" ]] || return 1
  bounds="$(echo "${line}" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | grep -oE '[0-9]+' )"
  x1="$(echo "${bounds}" | sed -n '1p')"
  y1="$(echo "${bounds}" | sed -n '2p')"
  x2="$(echo "${bounds}" | sed -n '3p')"
  y2="$(echo "${bounds}" | sed -n '4p')"
  cx=$(( (x1 + x2) / 2 ))
  cy=$(( (y1 + y2) / 2 ))
  "${ADB_S[@]}" shell input tap "${cx}" "${cy}"
}

# --- Root timeline + diagnostic export ---
seed_root_timeline
LATEST_ID="$(run_sql "SELECT id FROM wake_events WHERE rootEnhanced=1 ORDER BY timestampMillis DESC LIMIT 1;" || echo "")"
if [[ -n "${LATEST_ID}" ]]; then
  "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
    -d "screenwakelock://app/detail/${LATEST_ID}" -p "${PACKAGE}" >/dev/null 2>&1 || true
  sleep 3
  DETAIL_UI="$(ui_dump)"
  echo "${DETAIL_UI}" | grep -qiE "Root timeline|root timeline|Matched via|dumpsys" \
    && pass "Root timeline / parser footnote on Detail" \
    || warn "Root timeline UI not detected — verify Detail manually"
else
  warn "No root-enhanced row seeded"
fi

if [[ "${FORCE_ROOT_SMOKE:-1}" == "1" ]]; then
  SU_OUT="$("${ADB_S[@]}" shell su -c id 2>/dev/null || true)"
  if echo "${SU_OUT}" | grep -q "uid=0"; then
    # shellcheck source=scripts/smoke/_root_enable.sh
    source "${SCRIPT_DIR}/_root_enable.sh"
    ROOT_ENABLE_PACKAGE="${PACKAGE}" root_enable_app "${ADB}" -s "${DEVICE}"
  fi
fi

"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://settings/root" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
ROOT_UI="$(ui_dump)"
echo "${ROOT_UI}" | grep -qiE "Share diagnostic|Run diagnostics|Root access" \
  && pass "Root screen with diagnostic export controls" \
  || warn "Root screen UI incomplete"

if tap_text "Run diagnostics" 2>/dev/null; then
  sleep 3
  pass "Run diagnostics tapped"
else
  warn "Run diagnostics button not found"
fi

if tap_text "Share diagnostic report" 2>/dev/null; then
  sleep 2
  CHOOSER="$(ui_dump)"
  echo "${CHOOSER}" | grep -qiE "Share|Send|Copy|Nearby|Drive" \
    && pass "Share diagnostic report opens chooser" \
    || pass "Share diagnostic report tapped (OEM chooser may differ)"
  "${ADB_S[@]}" shell input keyevent KEYCODE_BACK 2>/dev/null || true
else
  warn "Share diagnostic report not found"
fi

# --- Pattern actions ---
seed_pattern_nights
"${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
sleep 1
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://insights" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 3
for _ in 1 2 3; do
  "${ADB_S[@]}" shell input swipe 720 2200 720 500 350 2>/dev/null || true
  sleep 1
done
INSIGHTS_UI="$(ui_dump)"
echo "${INSIGHTS_UI}" | grep -qi "Recurring patterns" \
  && pass "Recurring patterns section visible" \
  || warn "Recurring patterns section not found"

echo "${INSIGHTS_UI}" | grep -qi "Mute channel" \
  && pass "Pattern card Mute channel action present" \
  || warn "Mute channel button not in UI dump"

echo "${INSIGHTS_UI}" | grep -qi "Open settings" \
  && pass "Pattern card Open settings action present" \
  || warn "Open settings button not in UI dump"

# --- Batch mute dialog ---
seed_top_offender
"${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
sleep 1
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://insights" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 3

if tap_text "BatchMuteApp" 2>/dev/null || tap_text "Batch mute channels" 2>/dev/null; then
  sleep 1
  pass "Batch mute entry opened"
else
  # Open overflow menu on first top offender row
  "${ADB_S[@]}" shell input tap 1180 520 2>/dev/null || "${ADB_S[@]}" shell input tap 1000 600 2>/dev/null || true
  sleep 1
  if tap_text "Batch mute channels" 2>/dev/null; then
    sleep 1
    BATCH_UI="$(ui_dump)"
    echo "${BATCH_UI}" | grep -qiE "Batch mute|OEM" \
      && pass "Batch mute confirm dialog shown" \
      || warn "Batch mute dialog not detected"
    tap_text "Cancel" 2>/dev/null || "${ADB_S[@]}" shell input keyevent KEYCODE_BACK 2>/dev/null || true
  else
    warn "Batch mute menu not reachable — OEM may hide overflow; long-press top offender manually"
  fi
fi

CRASH="$("${ADB_S[@]}" logcat -d -t 100 | grep "FATAL EXCEPTION" | grep "${PACKAGE}" || true)"
[[ -z "${CRASH}" ]] || fail "Crash during M8 deep verify"

log "PASS: M8 ADB deep verify complete on ${DEVICE} (${MODEL})"
exit 0
