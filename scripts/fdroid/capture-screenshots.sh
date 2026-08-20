#!/usr/bin/env bash
# Capture F-Droid phone screenshots in ONBOARDING.md order.
# Usage: bash scripts/fdroid/capture-screenshots.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${ROOT}/fastlane/metadata/android/en-US/images/phoneScreenshots"
PACKAGE="${PACKAGE:-com.screenwakelock.detector}"

# shellcheck source=scripts/smoke/_device.sh
source "${ROOT}/scripts/smoke/_device.sh"
ADB="$(resolve_smoke_adb)"
DEVICE="$(pick_smoke_device "${ADB}")" || {
  echo "No authorized device — skipping capture. PNGs stay as-is in ${OUT}"
  exit 0
}

mkdir -p "${OUT}"
ADB_S=( "${ADB}" -s "${DEVICE}" )

capture() {
  local name="$1"
  local dest="${OUT}/${name}"
  "${ADB_S[@]}" exec-out screencap -p > "${dest}"
  echo "Wrote ${dest}"
}

open() {
  local uri="$1"
  "${ADB_S[@]}" shell am start -a android.intent.action.VIEW \
    -d "${uri}" -p "${PACKAGE}" >/dev/null 2>&1 || true
  sleep 2
}

echo "Capturing F-Droid screenshots on ${DEVICE}"
open "screenwakelock://onboarding/welcome"
capture "01-welcome.png"
open "screenwakelock://onboarding/how"
capture "02-how-it-works.png"
open "screenwakelock://onboarding/privacy"
capture "03-privacy.png"
open "screenwakelock://onboarding/root"
capture "04-root-onboarding.png"
open "screenwakelock://app"
capture "05-home.png"
open "screenwakelock://history"
capture "06-history.png"
open "screenwakelock://insights"
capture "07-insights.png"
open "screenwakelock://settings"
capture "08-settings.png"
open "screenwakelock://permissions"
capture "09-permissions.png"
echo "PASS: screenshots in ${OUT}"
