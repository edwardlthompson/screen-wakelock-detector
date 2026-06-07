#!/usr/bin/env bash
# Create GitLab project labels (requires glab authenticated).
set -euo pipefail

create_label() {
  local name="$1"
  local color="$2"
  local description="$3"
  if glab label list 2>/dev/null | grep -q "^${name}\b"; then
    echo "Label ${name} already exists"
  else
    glab label create "${name}" --color "${color}" --description "${description}"
  fi
}

create_label AGENT "#428BCA" "Agent implements autonomously"
create_label ADB "#FC6D26" "Needs USB device testing"
create_label HUMAN "#754778" "Needs human review"
create_label fdroid "#7BC043" "F-Droid pipeline"
create_label gate-blocked "#D9534F" "Gate failure"
create_label blocked "#ADB5BD" "External blocker"

for m in M0 M1 M2 M3 M4 M5 M6 M7; do
  create_label "milestone-${m}" "#C91E24" "Milestone ${m} work"
done

echo "GitLab labels ready"
