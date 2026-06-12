# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

---

---

## M12 — v1.2.9 app identification + QuickFix ignore

**Target:** `versionName` `1.2.9`, `versionCode` `1002009` in `app/build.gradle.kts`  
**Gate:** G12 in `docs/GATES.md` · Smoke: `scripts/smoke/m12_smoke.sh`  
**Plan:** ignore on QuickFix popup; hide ignored apps from History/Home/widgets; stronger unknown-app attribution (tag parse, UsageStats merge, display-time labels)

<!-- PARALLEL -->
- [x] [AGENT] [PARALLEL-OK] `PackageFromWakelockTag` + unit tests; wire into `WakeAttributor` / `RootAttributor` for tag-only root attribution
- [x] [AGENT] [PARALLEL-OK] Always merge UsageStats candidates in `WakeAttributor` (lower confidence when notifications exist)
- [x] [AGENT] [PARALLEL-OK] `AppDisplayResolver` — fresh PM labels + wakelock-tag fallbacks; use in UI, alerts, widgets
- [x] [AGENT] [PARALLEL-OK] `QuickFixBottomSheet`: **Ignore this app** button + undo snackbar; wire `HomeScreen` + `HistoryScreen` + `HomeViewModel` / `HistoryViewModel`
<!-- END PARALLEL -->

- [x] [AGENT] Filter ignored packages from `HistoryViewModel`, `HomeViewModel.latestWake`, `WakeCountWidget`, `WakeWidget`
- [x] [AGENT] Secondary ignore entry points: Detail undo snackbar, Insights offender menu, Detail candidate rows
- [x] [AGENT] Settings **Ignored apps** supporting copy — note ignored apps hidden from History; reversible via Remove
- [x] [AGENT] Low-confidence cards: show wakelock tag / root parser hint when name still unknown
- [x] [AGENT] Unit tests: `PackageFromWakelockTag`, `WakeAttributor`, `AppDisplayResolver`, History ignore filter
- [x] [AGENT] `scripts/smoke/m12_smoke.sh` — QuickFix ignore visible; ignored app absent from History
- [x] [AGENT] `./gradlew lint test assembleDebug`; CHANGELOG `[Unreleased]` + AGENT_MEMORY
- [x] [AGENT] Gate G12; archive M12; commit; push; tag `v1.2.9`; publish signed APK

### M12 — ADB

- [x] [ADB] Home **Fix it** popup shows **Ignore this app** for attributed wake; undo restores visibility
- [x] [ADB] After ignore, app wake no longer listed in History/Home; still removable in Settings → Ignored apps

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
