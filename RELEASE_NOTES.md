# Release notes — v1.2.16

**Date:** 2026-07-22  
**Device soak:** OP12 `b5214fc6` (CPH2583)

## Highlights

- **Wake Shield hardening** — L3 uses `KEYCODE_SLEEP` only (never `KEYCODE_POWER` toggle)
- Cooldown/self-wake armed **before** L1–L3 so shield actions cannot re-enter
- Longer rails (cooldown 10s / self-wake 5s)
- Shield decisions reuse the wake’s attribution — no second root `dumpsys` pass

## Why

Prevents wake/sleep storms and unnecessary CPU work if Wake Shield + root kill is armed on a noisy device.

## Verify

```bash
./gradlew lint test assembleDebug
bash scripts/smoke/m14_smoke.sh
bash scripts/smoke/m16_smoke.sh
```
