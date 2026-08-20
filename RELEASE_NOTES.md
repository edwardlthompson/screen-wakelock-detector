# Release notes — v1.2.18

**Date:** 2026-08-20  
**Device soak:** OP12 `b5214fc6` (CPH2583)

## Highlights

- Wake Shield week totals on Insights; widgets and Home last-wake show armed / last outcome
- Night-only ignore list; backup includes it; Insights can night-ignore or never-shield an offender
- Attribution fallback from wakelock tags and low-importance notifications; Home unknown-rate chip
- History chips for Unknown / Shielded / Allowed; optional weekly shield digest
- F-Droid listing pack: extra onboarding slides, capture script, feature graphic

## Why

Mystery wakes stay UNKNOWN too often, and shield impact is hard to see. Listing screenshots were incomplete for F-Droid.

## Verify

```bash
./gradlew lint test assembleDebug
bash scripts/smoke/m14_smoke.sh
bash scripts/smoke/m13_adb_verify.sh
bash scripts/benchmark/memory_baseline.sh
```
