#!/usr/bin/env bash
# M7 smoke: F-Droid metadata lint, bump script, prepare MR dry-run
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT}"

log() { echo "[m7_smoke] $*"; }
fail() { echo "[m7_smoke] FAIL: $*" >&2; exit 1; }

log "Lint F-Droid metadata"
bash scripts/fdroid/lint-metadata.sh

log "Bump metadata dry-run (read Gradle versions)"
python3 scripts/fdroid/bump-metadata.py --commit "v1.1.0-test" --dry-run

log "Prepare fdroiddata MR dry-run"
DRY_RUN=1 bash scripts/fdroid/prepare-fdroiddata-mr.sh

grep -q "CurrentVersion:" fdroid/metadata/com.screenwakelock.detector.yml \
  || fail "metadata missing CurrentVersion"
for f in \
  scripts/fdroid/verify-reproducible.sh \
  scripts/fdroid/open-fdroiddata-mr.sh \
  scripts/fdroid/prepare-fdroiddata-mr.sh \
  .github/workflows/fdroid-publish.yml \
  docs/F-DROID.md
do
  [[ -f "${f}" ]] || fail "missing ${f}"
done

log "Verify-reproducible fails without APKs (expected)"
if bash scripts/fdroid/verify-reproducible.sh 2>/dev/null; then
  fail "verify-reproducible should fail without APKs"
else
  log "verify-reproducible correctly rejected missing APKs"
fi

log "PASS: M7 smoke complete"
exit 0
