#!/usr/bin/env bash
# Fail if any tracked file exceeds size budget (matches pre-commit 500KB gate)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MAX_KB=500
MAX_BYTES=$((MAX_KB * 1024))
# Android store assets and design references (pre-existing product repo)
ALLOWLIST=(
  "fastlane/metadata/android/en-US/images/icon.png"
  "docs/design/icon-reference-512.png"
)
ERRORS=0
MAX_REPORT=20
reported=0

while IFS= read -r file; do
  [ -z "$file" ] && continue
  skip=false
  for allowed in "${ALLOWLIST[@]}"; do
    if [ "$file" = "$allowed" ]; then skip=true; break; fi
  done
  if [ "$skip" = true ]; then continue; fi
  size=$(git cat-file -s "HEAD:$file" 2>/dev/null || echo 0)
  if [ "$size" -gt "$MAX_BYTES" ]; then
    kb=$((size / 1024))
    echo "LARGE TRACKED FILE: $file (${kb} KB > ${MAX_KB} KB)"
    ERRORS=$((ERRORS + 1))
    reported=$((reported + 1))
    if [ "$reported" -ge "$MAX_REPORT" ]; then
      echo "... truncated (max $MAX_REPORT)"
      break
    fi
  fi
done < <(git ls-files)

if [ "$ERRORS" -gt 0 ]; then
  echo "$ERRORS tracked file(s) exceed ${MAX_KB} KB"
  exit 1
fi

echo "Large tracked file check passed"
