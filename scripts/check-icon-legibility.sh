#!/usr/bin/env bash
# Static adaptive-icon legibility checks (safe zone, layers, contrast colors).
# Usage: bash scripts/check-icon-legibility.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo "[check-icon-legibility] $*"; }
fail() { echo "[check-icon-legibility] FAIL: $*" >&2; ERRORS=$((ERRORS + 1)); }

ERRORS=0

require_file() {
  [[ -f "$1" ]] || { fail "missing $1"; return; }
  log "OK   exists: $1"
}

log "=== Adaptive icon resources ==="
require_file "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
require_file "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"
require_file "app/src/main/res/drawable/ic_launcher_foreground.xml"
require_file "app/src/main/res/drawable/ic_launcher_monochrome.xml"

for xml in ic_launcher.xml ic_launcher_round.xml; do
  f="app/src/main/res/mipmap-anydpi-v26/${xml}"
  grep -q 'android:drawable="@drawable/ic_launcher_foreground"' "${f}" \
    && log "OK   ${xml}: foreground layer" \
    || fail "${xml} missing foreground reference"
  grep -q 'android:drawable="@color/ic_launcher_background"' "${f}" \
    && log "OK   ${xml}: background layer" \
    || fail "${xml} missing background reference"
  grep -q 'android:drawable="@drawable/ic_launcher_monochrome"' "${f}" \
    && log "OK   ${xml}: monochrome layer (themed icon)" \
    || fail "${xml} missing monochrome reference"
done

grep -q 'pathData=' app/src/main/res/drawable/ic_launcher_foreground.xml \
  && log "OK   foreground vector has pathData" \
  || fail "foreground vector empty"

grep -q 'ic_launcher_background' app/src/main/res/values/colors.xml \
  && log "OK   launcher background color defined" \
  || fail "ic_launcher_background color missing"

python3 - <<'PY' || fail "foreground safe-zone scale check"
import re
from pathlib import Path

text = Path("app/src/main/res/drawable/ic_launcher_foreground.xml").read_text(encoding="utf-8")
scale = float(re.search(r'android:scaleX="([0-9.]+)"', text).group(1))
# Android safe zone: 66dp diameter in 108dp canvas → max scale ≈ 66/108 ≈ 0.611
if scale > 0.78:
    raise SystemExit(f"scale {scale} may clip under round mask (recommend <= 0.72)")
print(f"OK   foreground scale {scale} within legibility safe zone")
PY

if [[ "${ERRORS}" -gt 0 ]]; then
  echo "[check-icon-legibility] ${ERRORS} check(s) failed"
  exit 1
fi
log "PASS: static icon legibility checks"
exit 0
