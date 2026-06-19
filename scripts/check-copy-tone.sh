#!/usr/bin/env bash
# Static copy/tone gate for M2 (attribution), M4 (quick actions), M5 (insights/alerts).
# Usage: bash scripts/check-copy-tone.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo "[check-copy-tone] $*"; }
fail_msg() { echo "[check-copy-tone] FAIL: $*" >&2; ERRORS=$((ERRORS + 1)); }

ERRORS=0

require_in_file() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  [[ -f "${file}" ]] || { fail_msg "missing file ${file} (${label})"; return; }
  if grep -qE "${pattern}" "${file}"; then
    log "OK   ${label}"
  else
    fail_msg "${label} — expected /${pattern}/ in ${file}"
  fi
}

require_multiline_in_file() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  [[ -f "${file}" ]] || { fail_msg "missing file ${file} (${label})"; return; }
  if tr '\n' ' ' < "${file}" | grep -qE "${pattern}"; then
    log "OK   ${label}"
  else
    fail_msg "${label} — expected /${pattern}/ in ${file}"
  fi
}

log "=== M2 attribution copy ==="
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/DetailScreen.kt" \
  'Why this app\?' "M2 detail: Why this app?"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/DetailScreen.kt" \
  'Ranked candidates when attribution is uncertain' "M2 detail: ranked candidates rationale"
require_in_file "app/src/main/java/com/screenwakelock/detector/alerts/WakeAlertNotifier.kt" \
  'woke your screen' "M2/M5 alerts: offender-named threshold title"
require_in_file "app/src/main/java/com/screenwakelock/detector/alerts/WakeAlertNotifier.kt" \
  'Screen woke — app unknown' "M2 alerts: unknown wake honesty copy"
require_in_file "app/src/main/java/com/screenwakelock/detector/alerts/WakeAlertNotifier.kt" \
  'Enable Notification access' "M2 alerts: permission callout"
require_multiline_in_file "app/src/main/java/com/screenwakelock/detector/data/repository/PermissionStatusRepository.kt" \
  'Why:.*What:.*Never accesses' "M2 permissions: What/Why/Never in notification description"

log "=== M4 quick actions copy ==="
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/components/QuickFixBottomSheet.kt" \
  'Silence channel' "M4 quick-fix: Silence channel"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/components/QuickFixBottomSheet.kt" \
  'Open notification settings' "M4 quick-fix: Open notification settings"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/components/QuickFixBottomSheet.kt" \
  'Why this app\?' "M4 quick-fix: Why this app?"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/DetailScreen.kt" \
  'Open notification settings' "M4 detail: Open notification settings"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/HomeScreen.kt" \
  'Fix it' "M4 home: Fix it CTA"

log "=== M5 insights + threshold copy ==="
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/InsightsScreen.kt" \
  'Insights' "M5 insights: screen title"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/InsightsScreen.kt" \
  'Top offenders' "M5 insights: Top offenders section"
require_in_file "app/src/main/java/com/screenwakelock/detector/ui/screens/SettingsScreen.kt" \
  'wakes per hour' "M5 settings: threshold description"
require_in_file "app/src/main/java/com/screenwakelock/detector/alerts/WakeAlertNotifier.kt" \
  'times in the last hour' "M5 alerts: threshold body template"

log "=== Tone guardrails ==="
while IFS= read -r -d '' f; do
  if grep -qE 'Lorem ipsum|TODO copy|FIXME copy|TBD' "$f"; then
    fail_msg "placeholder copy in ${f#app/}"
  fi
done < <(find app/src/main/java/com/screenwakelock/detector/ui -name '*.kt' -print0)

if [[ "${ERRORS}" -gt 0 ]]; then
  echo "[check-copy-tone] ${ERRORS} check(s) failed"
  exit 1
fi

log "PASS: M2/M4/M5 copy/tone static checks"
exit 0
