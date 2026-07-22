#!/usr/bin/env bash
# M16 smoke: Wake Shield source checks (+ optional device)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[m16_smoke] $*"; }
fail() { echo "[m16_smoke] FAIL: $*" >&2; exit 1; }

[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/wakeshield/ShieldPolicy.kt" ]] \
  || fail "ShieldPolicy.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/wakeshield/ShieldCoordinator.kt" ]] \
  || fail "ShieldCoordinator.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/wakeshield/RootWakeEnforcer.kt" ]] \
  || fail "RootWakeEnforcer.kt missing"
[[ -f "${ROOT}/app/src/main/java/com/screenwakelock/detector/wakeshield/ShieldAccessibilityService.kt" ]] \
  || fail "ShieldAccessibilityService.kt missing"
[[ -f "${ROOT}/app/src/main/res/xml/shield_accessibility_config.xml" ]] \
  || fail "shield_accessibility_config.xml missing"

grep -q "INPUT_KEYCODE_SLEEP" \
  "${ROOT}/app/src/main/java/com/screenwakelock/detector/root/RootCommandAllowlist.kt" \
  || fail "Root allowlist missing KEYCODE_SLEEP"
grep -q "canRetrieveWindowContent=\"false\"" \
  "${ROOT}/app/src/main/res/xml/shield_accessibility_config.xml" \
  || fail "Accessibility config must not retrieve window content"
grep -q "ACTION_PANIC_DISABLE_SHIELD" \
  "${ROOT}/app/src/main/java/com/screenwakelock/detector/service/WakeMonitorService.kt" \
  || fail "Panic action missing from WakeMonitorService"
grep -q "MIGRATION_3_4" \
  "${ROOT}/app/src/main/java/com/screenwakelock/detector/data/db/AppDatabase.kt" \
  || fail "Room migration 3→4 missing"
grep -q "Wake Shield" "${ROOT}/CHANGELOG.md" || fail "CHANGELOG missing Wake Shield entry"
grep -q "Wake Shield" "${ROOT}/docs/ROOT.md" || fail "ROOT.md missing Wake Shield section"

log "Running unit tests for shield + root allowlist"
if [[ ! -x "${ROOT}/gradlew" && ! -f "${ROOT}/gradlew" ]]; then
  fail "gradlew missing"
fi
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then
  log "JAVA_HOME/java not available in this shell — skip embedded gradle (run ./gradlew testDebugUnitTest locally)"
else
  "${ROOT}/gradlew" :app:testDebugUnitTest \
    --tests "com.screenwakelock.detector.wakeshield.*" \
    --tests "com.screenwakelock.detector.root.RootCommandAllowlistTest" \
    -q || fail "gradle unit tests failed"
fi

log "M16 source checks PASS"

# Optional device install (best-effort; avoid sourcing CRLF helpers under WSL)
ADB_BIN="${ADB:-adb}"
DEVICE=""
if command -v "${ADB_BIN}" >/dev/null 2>&1; then
  DEVICE="$("${ADB_BIN}" devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
if [[ -n "${DEVICE}" ]]; then
  log "Device ${DEVICE} — verify shield service registered (APK must already be installed or assemble locally)"
  if "${ADB_BIN}" -s "${DEVICE}" shell dumpsys package com.screenwakelock.detector 2>/dev/null \
    | grep -q "ShieldAccessibilityService"; then
    log "Device package registration OK — HUMAN: arm shield, test alarm allow + panic"
  else
    log "ShieldAccessibilityService not on device yet — install debug APK then re-check"
  fi
else
  log "No device — skipping install (source checks PASS)"
fi

log "PASS: m16_smoke"
