# Completed Tasks

Archived milestone tasks are appended here by `scripts/archive-completed-tasks.py` after smoke validation passes.

## 2026-06-07 — M0 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Scaffold Kotlin/Compose app module and package `com.screenwakelock.detector`
- [x] [AGENT] [PARALLEL-OK] Add M3 theme foundation: Color, Type, Shape, Theme.kt with dynamic color + static fallback
- [x] [AGENT] [PARALLEL-OK] Add industry-standard README.md (12-section layout) + About string in docs/GITLAB.md
- [x] [AGENT] [PARALLEL-OK] Add scripts/smoke/m0_smoke.sh … m6_smoke.sh + ADB_TESTING.md smoke docs
- [x] [AGENT] [PARALLEL-OK] Add LICENSE (Apache-2.0), SECURITY.md, and full docs tree (FOSS, F-DROID, PRIVACY, AGENT_MEMORY, BUILD_PLAN, etc.)
- [x] [AGENT] [PARALLEL-OK] Add license-check CI + .cursor/rules FOSS/security constraints
- [x] [AGENT] [PARALLEL-OK] Add .cursor/rules/project.mdc: M3-only UI, link DESIGN_SYSTEM.md, no Material 2 imports
- [x] [AGENT] [PARALLEL-OK] Add .cursor/AGENTS.md (parallel, run-until-complete, smoke-before-archive)
- [x] [AGENT] [PARALLEL-OK] Implement archive-completed-tasks.py (requires smoke pass in GATES.md)
- [x] [AGENT] [PARALLEL-OK] Add .gitlab-ci.yml, .fdroid.yml, scripts/fdroid/, fdroid/metadata/ template
- [x] [ADB] Confirm adb devices sees test phone; run m0_smoke.sh PASS

## 2026-06-07 — M1 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Foreground monitoring service (ACTION_SCREEN_ON/OFF + DisplayManager)
- [x] [AGENT] [PARALLEL-OK] Room schema: WakeEvent entity + repository + DAO
- [x] [AGENT] [PARALLEL-OK] Basic history UI: M3 Scaffold, TopAppBar, LazyColumn, ElevatedCard rows, empty state
- [x] [AGENT] [PARALLEL-OK] Onboarding shell: Welcome, How it works (4 steps), Privacy; DataStore hasCompletedIntro
- [x] [AGENT] Wire first-launch routing through intro before Home
- [x] [AGENT] Unit tests for entity mapping + repository
- [x] [ADB] Screen-on events logged within 500 ms on test device
- [x] [HUMAN] Service survives Doze with exemption flow documented
- [x] [ADB] Run m1_smoke.sh PASS; record in GATES.md

## 2026-06-07 — M2 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] NotificationListenerService + Room notification cache (metadata only)
- [x] [AGENT] [PARALLEL-OK] Notification + Usage permission onboarding screens with grant intents
- [x] [AGENT] [PARALLEL-OK] Settings → Permissions screen (Notification + Usage switch rows)
- [x] [AGENT] WakeAttributor correlator (±N second window, reason codes)
- [x] [AGENT] Usage stats fallback when no notification match
- [x] [AGENT] Wake detail screen: confidence bar, “Why this app?”, ranked candidates when low confidence
- [x] [ADB] ≥80% notification-driven test wakes attributed to correct app+channel on reference device
- [x] [ADB] Run m2_smoke.sh PASS; record in GATES.md

## 2026-06-07 — M3 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] libsu + RootShellService + RootCommandAllowlist + RootCommandRunner
- [x] [AGENT] [PARALLEL-OK] DumpsysPowerParser, DumpsysBatteryStatsParser, WakeupSourcesParser + fixture tests
- [x] [AGENT] [PARALLEL-OK] Settings → Root: enable switch, diagnostics, grayed rows when not rooted
- [x] [AGENT] [PARALLEL-OK] Root informational onboarding step
- [x] [AGENT] RootAttributor merges wakelock snapshot into WakeEvent on screen-on
- [x] [AGENT] RootAvailability probe (su; Magisk/KernelSU labels for UI copy only)
- [x] [AGENT] Unit tests: allowlist rejects arbitrary commands; parser fixtures API 29/31/34
- [x] [ADB] Non-root: root rows disabled; no crash on su deny/timeout/parse failure
- [x] [ADB] Run m3_smoke.sh PASS; record in GATES.md

## 2026-06-07 — M4 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Last-wake home card + history row IconButton actions
- [x] [AGENT] [PARALLEL-OK] Swipe actions: settings / mute channel with confirm + Snackbar Undo
- [x] [AGENT] [PARALLEL-OK] Quick-fix ModalBottomSheet (silence, open settings, why this app?)
- [x] [AGENT] [PARALLEL-OK] WakeAlertNotifier with descriptive app+channel copy
- [x] [AGENT] Deep links: channel settings, app notification settings, app details
- [x] [AGENT] Mute channel via NotificationManager where supported; document OEM limits
- [x] [AGENT] Post-notifications onboarding step + Settings alert notifications switch row
- [x] [AGENT] Instrumentation tests for intent construction
- [x] [ADB] Run m4_smoke.sh PASS; record in GATES.md

## 2026-06-07 — M5 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Insights dashboard tab: top offenders, nighttime highlight, counts
- [x] [AGENT] [PARALLEL-OK] Threshold alerts (opt-in) + permission-missing callout variants
- [x] [AGENT] [PARALLEL-OK] Onboarding polish: verify setup, battery row, permission chips, replay wizard
- [x] [AGENT] [PARALLEL-OK] Search/filter: SearchBar, FilterChip, DatePickerDialog
- [x] [AGENT] [PARALLEL-OK] fastlane metadata + screenshots; fdroiddata MR prep
- [x] [AGENT] [PARALLEL-OK] Adaptive navigation for tablets/foldables
- [x] [AGENT] Reproducible build verify or document blockers in AGENT_MEMORY
- [x] [AGENT] Gate GS full: exported components, backup rules, no INTERNET
- [x] [ADB] Run m5_smoke.sh PASS; record in GATES.md

## 2026-06-07 — M6 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Glance/App Widget: last wake + Fix it deep link
- [x] [AGENT] [PARALLEL-OK] Quick Settings tile: pause/resume monitoring
- [x] [AGENT] [PARALLEL-OK] Pattern detection: recurring same app+channel ≥3 nights
- [x] [AGENT] [PARALLEL-OK] Time heatmap: 7-day hour×day grid with tap-to-filter
- [x] [AGENT] Unit test: heatmap matches history query for sample dataset
- [x] [ADB] Run m6_smoke.sh PASS; record in GATES.md
