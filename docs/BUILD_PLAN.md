# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

---

## M11 — v1.2.3 condensed onboarding + version-aware settings

**Target:** `versionName` `1.2.3`, `versionCode` `1002003` in `app/build.gradle.kts`  
**Gate:** G11 in `docs/GATES.md` · Smoke: `scripts/smoke/m11_smoke.sh`

<!-- PARALLEL -->
<!-- END PARALLEL -->

- [x] [AGENT] Archive M11; commit; push; tag `v1.2.3`; publish signed APK

### M11 — ADB

- [ ] [ADB] OP13 Lineage: restricted chip live refresh; 2-page onboarding; no Verify page

---

## M10 — v1.2.2 guided sideload permission setup

**Target:** `versionName` `1.2.2`, `versionCode` `1002002` in `app/build.gradle.kts`  
**Gate:** G10 in `docs/GATES.md` · Smoke: `scripts/smoke/m10_smoke.sh`

<!-- PARALLEL -->
<!-- END PARALLEL -->

- [ ] [AGENT] Gate G10; archive M10; commit; push; tag `v1.2.2`

### M10 — HUMAN

- [ ] [HUMAN] Verify OxygenOS 15 ⋮ menu vs SAI fallback copy on real OP13

---

## M9 — HUMAN (batched, milestone end)

- [ ] [HUMAN] Back up `%USERPROFILE%\.screen-wakelock-detector\release.jks` + passwords off-device
- [ ] [HUMAN] Optional: deprecate v1.2.0 release note — "Download v1.2.1 signed APK instead"

---

## Remaining work (HUMAN only)

- [ ] [HUMAN] GitLab project, MCP, README About, labels (`scripts/gitlab/create-labels.sh` ready)
- [ ] [HUMAN] M2/M4/M5 copy and design walkthroughs (ONBOARDING.md, DESIGN_SYSTEM.md)
- [ ] [HUMAN] 1.0.0-rc.1 soak; F-Droid first inclusion + `FDROIDDATA_FORK_URL` secret
- [ ] [HUMAN] Tag triggers live fdroiddata MR; app on F-Droid after merge
- [ ] [HUMAN] M8 icon legibility on device launcher — round/square masks, light/dark wallpapers

**M8:** Complete — tag [v1.2.0](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.0) published 2026-06-07.

**M9:** Complete — tag [v1.2.1](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.1) signed release published 2026-06-07.

**M10:** Complete — tag [v1.2.2](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.2) published 2026-06-07.

**M11:** Complete — tag [v1.2.3](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.3) published 2026-06-07.

See [`GATES.md`](GATES.md) for full checklists.
