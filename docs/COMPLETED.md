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
