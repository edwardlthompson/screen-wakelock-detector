#!/usr/bin/env bash
# Create GitLab project labels for Screen Wakelock Detector (see docs/GITLAB.md).
set -euo pipefail

GITLAB_HOST="${GITLAB_HOST:-gitlab.com}"
GITLAB_PROJECT="${GITLAB_PROJECT:-}" # numeric id or path e.g. you/screen-wakelock-detector
GITLAB_TOKEN="${GITLAB_TOKEN:-}"

log() { echo "[create-labels] $*"; }
fail() { echo "[create-labels] FAIL: $*" >&2; exit 1; }

[[ -n "${GITLAB_PROJECT}" ]] || fail "Set GITLAB_PROJECT (id or URL-encoded path)"
[[ -n "${GITLAB_TOKEN}" ]] || fail "Set GITLAB_TOKEN"

create_label() {
  local name="$1" color="$2" description="$3"
  local enc
  enc="$(python3 -c "import urllib.parse; print(urllib.parse.quote('${name}'))")"
  if curl -sf --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
    "https://${GITLAB_HOST}/api/v4/projects/${GITLAB_PROJECT}/labels/${enc}" >/dev/null 2>&1; then
    log "exists: ${name}"
    return 0
  fi
  curl -sf --request POST --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
    --data-urlencode "name=${name}" \
    --data-urlencode "color=${color}" \
    --data-urlencode "description=${description}" \
    "https://${GITLAB_HOST}/api/v4/projects/${GITLAB_PROJECT}/labels" >/dev/null
  log "created: ${name}"
}

create_label "AGENT" "#428BCA" "Agent implements autonomously"
create_label "ADB" "#FC6D26" "Needs USB device testing"
create_label "HUMAN" "#754778" "Needs human review"
create_label "fdroid" "#7BC043" "F-Droid pipeline"
create_label "gate-blocked" "#D9534F" "Gate failure"
create_label "blocked" "#ADB5BD" "External blocker"

for n in 0 1 2 3 4 5 6 7; do
  create_label "milestone-M${n}" "#C91E24" "Milestone M${n}"
done

log "DONE"
