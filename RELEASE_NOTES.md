# Release notes — v1.2.15

**Date:** 2026-07-21  
**Device soak:** OP13 `8bf09993` (CPH2655)

## Highlights

- **Wake Shield** — optional multi-tier wake firewall (forensics → notification cancel → Accessibility lock → root sleep/deny)
- Grace window (~1.5s); Unknown wakes treated as hostile after grace
- Panic disable on the monitoring notification; shield allowlist; Detail outcome banner

## Requirements for full enforcement

| Tier | Needs |
|------|--------|
| L0 forensics | Optional (on with root by default) |
| L1 cancel | Notification access |
| L2 re-lock | Accessibility (Wake Shield service) |
| L3 kill/deny | Magisk/KernelSU `su` + Root + Kill/deny toggles |

ADB root alone does not enable L3 — install Magisk (or KSU) and grant the app.

## Verify

```bash
./gradlew lint test assembleDebug
bash scripts/smoke/m16_smoke.sh
# with device:
bash scripts/smoke/m14_regression.sh
```
