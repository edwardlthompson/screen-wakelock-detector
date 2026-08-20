# Release notes — v1.2.17

**Date:** 2026-08-20  
**Device soak:** OP12 `b5214fc6` (CPH2583)

## Highlights

- Prefer the display’s fastest **same-resolution** mode on the activity window
- Mark History, Detail, Settings, Insights, Permissions, and Onboarding scroll surfaces **High** so adaptive panels can ramp during flings (API 35+)
- FOSS Cursor surface from agent-project-bootstrap v0.21.0 (hooks, skills, agents, worktrees, `/cleanup` `/coach` `/tour` `/ideas`)

## Why

120 Hz-class panels stay in a low mode unless the window picks a matching high-refresh mode and scroll surfaces vote High. Tooling catch-up keeps agent gates aligned with upstream without replacing Android CI.

## Verify

```bash
./gradlew lint test assembleDebug
bash scripts/smoke/m14_smoke.sh
bash scripts/benchmark/memory_baseline.sh
```
