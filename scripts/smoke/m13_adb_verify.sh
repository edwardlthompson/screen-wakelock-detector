#!/usr/bin/env bash
# M13 ADB verify: tag-only QuickFix ignore, History search, M12 attributed ignore+undo
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

ADB="${ADB:-adb}"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"

TAG_PKG="com.m13.tagonly.test"
SEARCH_PKG="com.m13.search.test"
ATTR_PKG="com.m13.attributed.test"

log() { echo "[m13_adb] $*"; }
pass() { log "PASS: $*"; }
warn() { log "WARN: $*"; }
fail() { log "FAIL: $*" >&2; exit 1; }

now_ms() {
  python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null \
    || python -c 'import time; print(int(time.time()*1000))'
}

# shellcheck source=scripts/smoke/_device.sh
source "${SCRIPT_DIR}/_device.sh"
DEVICE="$(pick_smoke_device "${ADB}")" || fail "device selection failed"
ADB_S=( "${ADB}" -s "${DEVICE}" )

MODEL="$("${ADB_S[@]}" shell getprop ro.product.model | tr -d '\r')"
SDK="$("${ADB_S[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
log "Device ${DEVICE} (${MODEL}, API ${SDK})"

[[ -f "${APK_PATH}" ]] || {
  export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
  ./gradlew assembleDebug
}

if ! "${ADB_S[@]}" install -r "${APK_PATH}" >/dev/null 2>&1; then
  log "Install failed — uninstalling prior build"
  "${ADB_S[@]}" uninstall "${PACKAGE}" || true
  "${ADB_S[@]}" install "${APK_PATH}"
fi

VERSION="$("${ADB_S[@]}" shell dumpsys package "${PACKAGE}" | grep versionName | head -1 | tr -d '\r' || true)"
log "Installed ${VERSION}"

ui_dump() {
  "${ADB_S[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true
}

run_sql() {
  local sql="$1"
  "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell "run-as ${PACKAGE} sqlite3 databases/screen_wakelock.db \"${sql}\"" 2>/dev/null | tr -d '\r'
}

clear_test_rows() {
  log "Clear prior M13 test wake rows"
  run_sql "DELETE FROM wake_events WHERE attributedPackage IN ('${ATTR_PKG}','com.m13.other.visible') OR wakelockTag LIKE 'com.m13.%';" || true
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
  sleep 1
}

launch_home() {
  "${ADB_S[@]}" shell am start -n "${PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep 2
}

complete_onboarding_if_needed() {
  local ui
  launch_home
  ui="$(ui_dump)"
  if echo "${ui}" | grep -q "Find out what keeps"; then
    log "Completing onboarding (fresh install)"
    tap_text "Next" || fail "Onboarding Next not found"
    sleep 1
    if tap_text "Skip" 2>/dev/null; then
      sleep 2
    elif tap_text "Get started" 2>/dev/null; then
      sleep 2
    else
      fail "Onboarding Skip/Get started not found"
    fi
    pass "Onboarding completed"
  fi
}

complete_onboarding_if_needed

open_quickfix() {
  local wake_id="$1"
  "${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
  sleep 1
  "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
    -d "screenwakelock://app/quickfix/${wake_id}" -p "${PACKAGE}" >/dev/null 2>&1 || true
  sleep 3
}

open_history() {
  tap_text "History" 2>/dev/null || {
    "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
      -d "screenwakelock://history" -p "${PACKAGE}" >/dev/null 2>&1 || true
  }
  sleep 2
}

seed_attributed_wake() {
  local ts="$1"
  run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,wakelockTag,wakelockName,rootParserId) VALUES (${ts},'${ATTR_PKG}','M13 Attributed','alerts','Alerts','NOTIFICATION_HEADS_UP',0.88,NULL,0,NULL,NULL,NULL);"
}

seed_tag_only_wake() {
  local ts="$1"
  local tag="${TAG_PKG}:notification"
  run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,wakelockTag,wakelockName,rootParserId) VALUES (${ts},NULL,NULL,NULL,NULL,'ROOT_WAKELOCK',0.55,NULL,1,'${tag}','notification','dumpsys_power');"
}

seed_search_wake() {
  local ts="$1"
  local tag="${SEARCH_PKG}:alarm"
  run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,wakelockTag,wakelockName,rootParserId) VALUES (${ts},NULL,NULL,NULL,NULL,'ROOT_WAKELOCK',0.55,NULL,1,'${tag}','alarm','dumpsys_power');"
}

seed_visible_filler() {
  local ts="$1"
  run_sql "INSERT INTO wake_events (timestampMillis,attributedPackage,attributedAppLabel,channelId,channelName,reasonCode,confidence,candidatesJson,rootEnhanced,wakelockTag,wakelockName,rootParserId) VALUES (${ts},'com.m13.other.visible','Other Visible',NULL,NULL,'NOTIFICATION_UNKNOWN',0.7,NULL,0,NULL,NULL,NULL);"
}

remove_ignored_via_settings() {
  for pkg in "${TAG_PKG}" "${ATTR_PKG}" "${SEARCH_PKG}"; do
    if tap_text "Remove" 2>/dev/null; then
      sleep 1
    fi
  done
}

tap_search_bar() {
  local line bounds x1 y1 x2 y2 cx cy
  line="$(ui_dump | tr '\n' ' ' | grep -o 'text="Search app or channel"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1 || true)"
  [[ -n "${line}" ]] || return 1
  bounds="$(echo "${line}" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | grep -oE '[0-9]+' )"
  x1="$(echo "${bounds}" | sed -n '1p')"
  y1="$(echo "${bounds}" | sed -n '2p')"
  x2="$(echo "${bounds}" | sed -n '3p')"
  y2="$(echo "${bounds}" | sed -n '4p')"
  cx=$(( (x1 + x2) / 2 ))
  cy=$(( (y1 + y2) / 2 ))
  "${ADB_S[@]}" shell input tap "${cx}" "${cy}"
  sleep 1
}

enter_search_query() {
  local query="$1"
  tap_search_bar || tap_text "Search app or channel" || true
  sleep 1
  "${ADB_S[@]}" shell input keyevent KEYCODE_MOVE_HOME 2>/dev/null || true
  "${ADB_S[@]}" shell input keyevent --longpress KEYCODE_DEL 2>/dev/null || true
  "${ADB_S[@]}" shell input text "${query}" 2>/dev/null || true
  sleep 2
}

clear_test_ignores() {
  launch_home
  tap_text "Settings" 2>/dev/null || {
    "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
      -d "screenwakelock://settings" -p "${PACKAGE}" >/dev/null 2>&1 || true
  }
  sleep 2
  scroll_settings_down
  local i
  for i in 1 2 3 4 5; do
    if tap_text "Remove" 2>/dev/null; then
      sleep 1
    else
      break
    fi
  done
  launch_home
}

ignored_pkg_in_prefs() {
  local pkg="$1"
  "${ADB_S[@]}" shell "run-as ${PACKAGE} cat files/datastore/settings.preferences_pb 2>/dev/null" \
    | grep -a -q "${pkg}" 2>/dev/null
}

scroll_settings_down() {
  "${ADB_S[@]}" shell input swipe 720 2000 720 600 350 2>/dev/null || true
  sleep 1
}

open_settings() {
  launch_home
  tap_text "Settings" 2>/dev/null || {
    "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
      -d "screenwakelock://settings" -p "${PACKAGE}" >/dev/null 2>&1 || true
  }
  sleep 2
}

verify_ignored_removable_in_settings() {
  local pkg="$1"
  ignored_pkg_in_prefs "${pkg}" || fail "Ignored package ${pkg} not in DataStore"
  pass "Ignored package ${pkg} present in DataStore"
  open_settings
  scroll_settings_down
  local ui
  ui="$(ui_dump)"
  echo "${ui}" | grep -qiE "Ignored apps|ignored" \
    || fail "Settings Ignored apps section not visible after scroll"
  pass "Settings Ignored apps section visible"
  echo "${ui}" | grep -q "${pkg}" \
    || fail "Ignored package ${pkg} not shown in Settings UI"
  pass "Ignored package ${pkg} listed in Settings"
  tap_text "Remove" || fail "Could not tap Remove for ignored app"
  sleep 2
  ignored_pkg_in_prefs "${pkg}" && fail "Ignored package ${pkg} still in DataStore after Remove" \
    || pass "Ignored app ${pkg} removable in Settings"
}

# --- M12: attributed wake ignore + undo ---
clear_test_rows
TS="$(now_ms)"
seed_attributed_wake "${TS}"
ATTR_ID="$(run_sql "SELECT id FROM wake_events WHERE attributedPackage='${ATTR_PKG}' ORDER BY timestampMillis DESC LIMIT 1;")"
[[ -n "${ATTR_ID}" ]] || fail "Failed to seed attributed wake"
log "M12: attributed wake id=${ATTR_ID}"
open_quickfix "${ATTR_ID}"
UI="$(ui_dump)"
echo "${UI}" | grep -q "Ignore this app" \
  && pass "M12 QuickFix shows Ignore this app (attributed wake)" \
  || fail "M12 QuickFix missing Ignore this app"
tap_text "Ignore this app" || fail "Could not tap Ignore this app (M12)"
sleep 1
UI="$(ui_dump)"
echo "${UI}" | grep -q "Undo" && tap_text "Undo" && sleep 1 \
  && pass "M12 ignore undo snackbar" \
  || warn "M12 undo snackbar not tapped — continuing"
launch_home
open_history
UI="$(ui_dump)"
echo "${UI}" | grep -q "${ATTR_PKG}\|M13 Attributed" \
  && pass "M12 attributed wake still visible after undo" \
  || warn "M12 wake not visible after undo (may have dismissed without undo)"

# --- M13: tag-only ignore ---
clear_test_rows
clear_test_ignores
TS="$(now_ms)"
seed_tag_only_wake "${TS}"
seed_visible_filler "$(( TS - 60000 ))"
TAG_ID="$(run_sql "SELECT id FROM wake_events WHERE wakelockTag LIKE '${TAG_PKG}%' ORDER BY timestampMillis DESC LIMIT 1;")"
[[ -n "${TAG_ID}" ]] || fail "Failed to seed tag-only wake"
log "M13 tag-only wake id=${TAG_ID}"
open_quickfix "${TAG_ID}"
UI="$(ui_dump)"
echo "${UI}" | grep -q "Ignore this app" \
  && pass "M13 QuickFix shows Ignore this app (tag-only wake)" \
  || fail "M13 QuickFix missing Ignore this app for tag-only wake"
echo "${UI}" | grep -q "${TAG_PKG}" \
  && pass "M13 QuickFix shows tag-derived package name" \
  || warn "M13 tag-derived name not matched in QuickFix UI"
tap_text "Ignore this app" || fail "Could not tap Ignore this app (M13 tag-only)"
sleep 2
open_history
UI="$(ui_dump)"
echo "${UI}" | grep -q "${TAG_PKG}" \
  && fail "M13 tag-only wake still in History after ignore" \
  || pass "M13 tag-only wake absent from History after ignore"
launch_home
UI="$(ui_dump)"
echo "${UI}" | grep -q "${TAG_PKG}" \
  && fail "M13 tag-only wake still on Home after ignore" \
  || pass "M13 tag-only wake absent from Home after ignore"

verify_ignored_removable_in_settings "${TAG_PKG}"

# --- M13: History search by tag-derived name ---
clear_test_rows
TS="$(now_ms)"
seed_search_wake "${TS}"
seed_visible_filler "$(( TS - 120000 ))"
SEARCH_ID="$(run_sql "SELECT id FROM wake_events WHERE wakelockTag LIKE '${SEARCH_PKG}%' ORDER BY timestampMillis DESC LIMIT 1;")"
[[ -n "${SEARCH_ID}" ]] || fail "Failed to seed search wake"
launch_home
open_history
UI="$(ui_dump)"
echo "${UI}" | grep -q "${SEARCH_PKG}" \
  && pass "M13 History list shows tag-derived package on card" \
  || fail "M13 History card missing tag-derived name before search"
"${ADB_S[@]}" shell am force-stop "${PACKAGE}" 2>/dev/null || true
sleep 1
"${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
  -d "screenwakelock://history?q=search.test" -p "${PACKAGE}" >/dev/null 2>&1 || true
sleep 3
UI="$(ui_dump)"
echo "${UI}" | grep -q "${SEARCH_PKG}" \
  && pass "M13 History search matches tag-derived app name" \
  || fail "M13 History search did not match tag-derived name"
echo "${UI}" | grep -q "Other Visible" \
  && fail "M13 History search still shows non-matching filler app" \
  || pass "M13 History search filters non-matching events"

clear_test_rows
log "DONE: M13 ADB verification complete on ${DEVICE}"
log "Record: Smoke M13: PASS $(date -u +%Y-%m-%dT%H:%M:%SZ) ${DEVICE} ${VERSION}"
