#!/usr/bin/env bash
# Verify branch protection on main (classic API or rulesets fallback).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo "[verify-branch-protection] $*"; }
fail() { echo "[verify-branch-protection] FAIL: $*" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI required"
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

REPO="${GITHUB_REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
BRANCH="${GITHUB_DEFAULT_BRANCH:-main}"
REQUIRED="${GITHUB_REQUIRED_CHECKS:-Android CI}"

log "Checking ${REPO} @ ${BRANCH} (required: ${REQUIRED})"

if gh api "repos/${REPO}/branches/${BRANCH}/protection" >/tmp/swd-bp.json 2>/dev/null; then
  python3 - <<PY "${REQUIRED}" /tmp/swd-bp.json
import json, sys
required = [c.strip() for c in sys.argv[1].split(",") if c.strip()]
data = json.load(open(sys.argv[2]))
contexts = data.get("required_status_checks", {}).get("contexts") or []
strict = data.get("required_status_checks", {}).get("strict")
force = data.get("allow_force_pushes", {}).get("enabled")
errors = []
for c in required:
    if c not in contexts:
        errors.append(f"missing required check context: {c!r} (have {contexts})")
if strict is not True:
    errors.append(f"strict required_status_checks expected true, got {strict!r}")
if force is True:
    errors.append("allow_force_pushes must be false")
if errors:
    for e in errors:
        print(f"FAIL: {e}")
    sys.exit(1)
print("OK   classic branch protection")
for c in required:
    print(f"     required check: {c}")
print(f"     strict: {strict}, force_push: {force}")
PY
  rm -f /tmp/swd-bp.json
  exit 0
fi

log "Classic protection 404 — checking rulesets"
RULESETS="$(gh api "repos/${REPO}/rulesets" 2>/dev/null || echo '[]')"
python3 - <<PY "${REQUIRED}" "${BRANCH}" "${RULESETS}"
import json, sys
required = [c.strip() for c in sys.argv[1].split(",") if c.strip()]
branch = sys.argv[2]
rulesets = json.loads(sys.argv[3])
active = [r for r in rulesets if r.get("enforcement") == "active"]
if not active:
    print("FAIL: no active rulesets and no classic branch protection")
    print("      Configure Settings -> Branches or Rules -> Rulesets")
    sys.exit(1)
print(f"OK   {len(active)} active ruleset(s) — manual confirm required checks include:")
for c in required:
    print(f"     - {c}")
print("     Settings -> Rules -> Rulesets -> verify block force push on main")
PY
exit 0
