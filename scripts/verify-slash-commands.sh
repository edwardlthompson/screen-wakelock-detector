#!/usr/bin/env bash
# Verify all 25 bootstrap slash command files exist and batch-commands.mdc registry is complete.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

COMMANDS=(
  audit debug gates triage dependabot push prerelease regress
  feature fix init prune ci docs upgrade setup plan restore compact scope
  bootstrap verify build ship maintain
)

ERRORS=0
for cmd in "${COMMANDS[@]}"; do
  if [ ! -f ".cursor/commands/${cmd}.md" ]; then
    echo "MISSING: .cursor/commands/${cmd}.md"
    ERRORS=$((ERRORS + 1))
  fi
done

if [ ! -f .cursor/rules/batch-commands.mdc ]; then
  echo "MISSING: .cursor/rules/batch-commands.mdc"
  ERRORS=$((ERRORS + 1))
fi

if ! bash scripts/check-batch-commands.sh >/dev/null 2>&1; then
  echo "FAIL: check-batch-commands.sh"
  ERRORS=$((ERRORS + 1))
fi

if [ "$ERRORS" -gt 0 ]; then
  echo "$ERRORS slash command check(s) failed"
  exit 1
fi

echo "Slash commands OK (25 files + batch-commands.mdc)"
