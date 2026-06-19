#!/usr/bin/env bash
# Idempotent GitHub repo security + branch protection for Screen Wakelock Detector.
# SWD uses workflow "Android CI" (job validate-and-build) — not template CI/CodeQL/Trivy.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo "[setup-github-repo] $*"; }
warn() { echo "[setup-github-repo] WARN: $*" >&2; }
fail() { echo "[setup-github-repo] FAIL: $*" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI required — https://cli.github.com/"
gh auth status >/dev/null 2>&1 || fail "gh not authenticated — run: gh auth login"

REPO="${GITHUB_REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)}"
[[ -n "${REPO}" ]] || fail "Set GITHUB_REPO=owner/name or run from a gh-linked clone"

DEFAULT_BRANCH="${GITHUB_DEFAULT_BRANCH:-main}"
# Status check context as shown on PRs (workflow rollup name).
REQUIRED_CHECKS="${GITHUB_REQUIRED_CHECKS:-Android CI}"

log "Repository: ${REPO} (branch ${DEFAULT_BRANCH})"
log "Required status checks: ${REQUIRED_CHECKS}"

print_manual_checklist() {
  cat <<EOF

Manual checklist (Settings -> Code security and analysis / Branches):
  [ ] Dependabot alerts + Dependabot security updates enabled
  [ ] Secret scanning + push protection enabled
  [ ] Private vulnerability reporting enabled
  [ ] Branch ${DEFAULT_BRANCH}: require status check "${REQUIRED_CHECKS}", block force push

Re-run: bash scripts/setup-github-repo.sh
Verify: bash scripts/verify-branch-protection.sh

EOF
}

enable_security_feature() {
  local feature="$1"
  local status="${2:-enabled}"
  if gh api -X PATCH "repos/${REPO}" \
    -f "security_and_analysis[${feature}][status]=${status}" >/dev/null 2>&1; then
    log "security: ${feature}=${status}"
    return 0
  fi
  warn "Could not set ${feature} via API (plan/permissions?) — use manual checklist"
  return 1
}

SECURITY_OK=0
enable_security_feature "dependabot_security_updates" "enabled" && SECURITY_OK=1 || true
enable_security_feature "secret_scanning" "enabled" && SECURITY_OK=1 || true
enable_security_feature "secret_scanning_push_protection" "enabled" && SECURITY_OK=1 || true

# Private vulnerability reporting (separate endpoint on some API versions)
if gh api -X PUT "repos/${REPO}/private_vulnerability_reporting" \
  -f enabled=true >/dev/null 2>&1; then
  log "private vulnerability reporting enabled"
elif gh api "repos/${REPO}/private_vulnerability_reporting" -q .enabled 2>/dev/null | grep -q true; then
  log "private vulnerability reporting already enabled"
else
  warn "private vulnerability reporting — enable in Settings -> Code security"
fi

# Dependabot alerts (advanced security analysis)
if gh api -X PATCH "repos/${REPO}" \
  -f "security_and_analysis[dependabot_alerts][status]=enabled" >/dev/null 2>&1; then
  log "dependabot alerts enabled"
else
  warn "dependabot alerts — confirm in Settings -> Code security"
fi

IFS=',' read -ra CHECKS <<< "${REQUIRED_CHECKS}"
CONTEXTS_JSON="$(printf '%s\n' "${CHECKS[@]}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | grep -v '^$' | \
  python3 -c 'import json,sys; print(json.dumps([l.strip() for l in sys.stdin if l.strip()]))')"

PROTECTION_PAYLOAD="$(python3 - <<PY
import json
contexts = json.loads('''${CONTEXTS_JSON}''')
print(json.dumps({
  "required_status_checks": {
    "strict": True,
    "contexts": contexts,
  },
  "enforce_admins": False,
  "required_pull_request_reviews": None,
  "restrictions": None,
  "allow_force_pushes": False,
  "allow_deletions": False,
  "required_linear_history": False,
}))
PY
)"

if gh api -X PUT "repos/${REPO}/branches/${DEFAULT_BRANCH}/protection" \
  --input - <<< "${PROTECTION_PAYLOAD}" >/dev/null 2>&1; then
  log "branch protection applied on ${DEFAULT_BRANCH}"
else
  warn "classic branch protection API failed (422/404?) — may use rulesets; verify manually"
  print_manual_checklist
fi

# About block from docs/GITHUB_ABOUT.md when present
ABOUT_FILE="${ROOT}/docs/GITHUB_ABOUT.md"
if [[ -f "${ABOUT_FILE}" ]]; then
  DESC="$(python3 - <<'PY' "${ABOUT_FILE}"
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
m = re.search(r"## Description \(GitHub About[^\n]*\n\n```text\n(.*?)\n```", text, re.S)
if m:
    print(m.group(1).strip())
PY
)"
  TOPICS="$(python3 - <<'PY' "${ABOUT_FILE}"
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
m = re.search(r"## Topics\n\n(.*?)(?:\n##|\Z)", text, re.S)
if not m:
    sys.exit(0)
for line in m.group(1).splitlines():
    line = line.strip().lstrip("- ").strip()
    if line and not line.startswith("#"):
        print(line)
PY
)"
  if [[ -n "${DESC:-}" ]]; then
    TOPIC_ARGS=()
    while IFS= read -r topic; do
      [[ -n "${topic}" ]] && TOPIC_ARGS+=(--add-topic "${topic}")
    done <<< "${TOPICS:-}"
    if gh repo edit "${REPO}" --description "${DESC}" "${TOPIC_ARGS[@]}" 2>/dev/null; then
      log "GitHub About description + topics updated from docs/GITHUB_ABOUT.md"
    else
      warn "gh repo edit failed — update About manually"
    fi
  fi
fi

[[ -f "${ROOT}/.github/dependabot.yml" ]] && log "dependabot.yml present" || warn "missing .github/dependabot.yml"

log "DONE — run: bash scripts/verify-branch-protection.sh"
if [[ "${SECURITY_OK}" -eq 0 ]]; then
  print_manual_checklist
  exit 1
fi
exit 0
