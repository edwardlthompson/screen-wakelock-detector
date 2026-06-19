#!/usr/bin/env bash
# Print count of open Critical/High Dependabot alerts (stdout: integer).
# Exit 1 on gh/API failure.
set -euo pipefail

command -v gh >/dev/null 2>&1 || exit 1

REPO="${GITHUB_REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)}"
[[ -n "${REPO}" ]] || exit 1

gh api "repos/${REPO}/dependabot/alerts?state=open" --paginate \
  --jq '[.[] | select(.security_vulnerability.severity == "critical" or .security_vulnerability.severity == "high")] | length' \
  2>/dev/null || exit 1
