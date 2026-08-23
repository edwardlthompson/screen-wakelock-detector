# Release notes — v1.2.19

**Date:** 2026-08-22  
**Device soak:** OP12 `b5214fc6` (CPH2583)

## Highlights

- Quiet Donate via Venmo; daily GitHub latest-release check from APK filenames (opt-out + Check now)
- Shield dry-run (would-have-blocked), wind-down, user grace, QS tile to arm/disarm
- Home tonight strip, cooldown/self-wake, morning digest, tablet Home+Detail two-pane
- History night / exempt / root filters; Detail candidate picker, never-tonight, L3 deny undo
- Life360/Health tag dictionary; Lineage 16 dumpsys fixture; Insights compare + export one offender

## Why

Users needed a F-Droid-safe update path and Venmo donate without mixing them, plus Shield preview and tonight-focused surfaces so dry-run and night filters are usable before a soak.

## Verify

```bash
./gradlew lint test assembleDebug
bash scripts/smoke/m14_smoke.sh
bash scripts/smoke/m13_adb_verify.sh
bash scripts/benchmark/memory_baseline.sh
```
