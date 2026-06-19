#!/usr/bin/env bash
# Screen Wakelock Detector — bootstrap validation (android child repo profile)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

QUICK=false
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=true ;;
  esac
done

REQUIRED=(
  README.md
  LICENSE
  CONTRIBUTING.md
  SECURITY.md
  CODE_OF_CONDUCT.md
  BUILD_PLAN.md
  AGENTS.md
  AGENT_MEMORY.md
  DECISION_LOG.md
  COMPLETED_TASKS.md
  docs/START_HERE.md
  docs/CURSOR_MODES.md
  docs/FOR_AGENTS.md
  docs/INITIALIZATION_PROMPT.md
  docs/BOOTSTRAP_TEMPLATE_MAP.md
  .cursor/rules/cursor-modes.mdc
  .cursor/rules/batch-commands.mdc
  docs/DESIGN_GUIDE.md
  docs/DESIGN_SYSTEM.md
  docs/SECURITY_TRIAGE.md
  docs/THREAT_MODEL.md
  docs/PRIVACY.md
  docs/RUNBOOK.md
  docs/FEATURE_MODULES.md
  docs/GATES.md
  .github/dependabot.yml
  .github/CODEOWNERS
  THIRD_PARTY_LICENSES.md
  .env.example
  .template-version
  docs/help/BATCH_COMMANDS.md
  docs/BATCH_COMMANDS.md
  modules/android/MODULE.md
  gradlew
  app/build.gradle.kts
)

BATCH_COMMANDS=(
  audit debug gates triage dependabot push prerelease regress
  feature fix init prune ci docs upgrade setup plan restore compact scope
  bootstrap verify build ship maintain
)

for cmd in "${BATCH_COMMANDS[@]}"; do
  REQUIRED+=(".cursor/commands/${cmd}.md")
done

ERRORS=0

run_check() {
  if ! "$@"; then
    ERRORS=$((ERRORS + 1))
  fi
}

for f in "${REQUIRED[@]}"; do
  if [ ! -e "$f" ]; then
    echo "MISSING: $f"
    ERRORS=$((ERRORS + 1))
  fi
done

if [ -f LICENSE ] && [ ! -s LICENSE ]; then
  echo "EMPTY: LICENSE"
  ERRORS=$((ERRORS + 1))
fi

if ! grep -qE '\[(AGENT|HUMAN|ADB)\]' BUILD_PLAN.md; then
  echo "MISSING: BUILD_PLAN.md owner labels"
  ERRORS=$((ERRORS + 1))
fi

bash scripts/sync-exemplar-config.sh >/dev/null 2>&1 || true
run_check bash scripts/check-file-encoding.sh
run_check bash scripts/check-markdown-tables.sh
run_check bash scripts/check-changelog-unreleased.sh
run_check bash scripts/check-repo-hygiene.sh
run_check bash scripts/check-batch-commands.sh

if [ "$QUICK" = false ]; then
  run_check bash scripts/check-readme-health.sh
fi

if [ "$ERRORS" -gt 0 ]; then
  echo "$ERRORS bootstrap check(s) failed"
  exit 1
fi

if [ "$QUICK" = true ]; then
  echo "Bootstrap validation passed (--quick)"
else
  echo "Bootstrap validation passed"
fi
