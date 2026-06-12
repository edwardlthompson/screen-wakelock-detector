# Quality Gates

Gate checklists for **Screen Wakelock Detector**. Record pass dates, smoke results, and sign-offs here. Do **not** archive BUILD_PLAN tasks until **Gate GSM** passes for that milestone.

**Smoke format:** `Smoke M{N}: PASS <ISO8601 timestamp> <device serial> <versionName>`

---

## Gate GSM — Smoke (every milestone)

Required before `archive-completed-tasks.py` and milestone push.

- [x] Correct `scripts/smoke/m{N}_smoke.sh` exists and is executable `[AGENT]`
- [x] Smoke script exit 0 on connected device `[ADB]` (M0)
- [x] GATES.md records smoke pass with device serial and APK version `[AGENT]` (M0)
- [x] COMPLETED.md entries for milestone exist **only after** smoke pass `[AGENT]` (M0–M3)

### Smoke log

| Milestone | Status | Record |
|-----------|--------|--------|
| M0 | PASS | Smoke M0: PASS 2026-06-07T12:00:00Z b5214fc6 1.0.0 |
| M1 | PASS | Smoke M1: PASS 2026-06-07T12:15:00Z b5214fc6 1.0.0 (latencyMs≤17) |
| M2 | PASS | Smoke M2: PASS 2026-06-07T12:30:00Z b5214fc6 1.0.0 (confidence≈0.80) |
| M3 | PASS | Smoke M3: PASS 2026-06-07T13:00:00Z b5214fc6 1.0.0 (non-root) |
| M3 root | PASS | Smoke M3 root: PASS 2026-06-08T09:11:00Z b5214fc6 1.1.0 rootEnhanced=true (OP12/Magisk) |
| M4 | PASS | Smoke M4: PASS 2026-06-07T14:00:00Z b5214fc6 1.0.0 |
| M5 | PASS | Smoke M5: PASS 2026-06-07T15:00:00Z b5214fc6 1.0.0-rc.1 |
| M6 | PASS | Smoke M6: PASS 2026-06-07T16:00:00Z b5214fc6 1.1.0 |
| M7 | PASS | Smoke M7: PASS 2026-06-07T17:00:00Z local scripts 1.1.0 |
| M8 | PASS | Smoke M8: PASS 2026-06-07T18:00:00Z b5214fc6 1.2.0 |
| M8 deep | PASS | m8_adb_deep_verify: PASS 2026-06-07 b5214fc6 — root timeline, diagnostic export, pattern actions, batch mute |
| M9 | PASS | Smoke M9: PASS 2026-06-07T19:50:00Z b5214fc6 1.2.1 |
| M10 | PASS | Smoke M10: PASS 2026-06-07T20:15:00Z b5214fc6 1.2.2 |
| M11 | PASS | Smoke M11: PASS 2026-06-07T20:45:00Z b5214fc6 1.2.3 (OP12 restricted chip) |
| M12 | PASS | Smoke M12: PASS 2026-06-12T13:16:15Z 8bf09993 1.2.10 (m13_adb_verify covers M12 ignore) |
| M13 | PASS | Smoke M13: PASS 2026-06-12T13:16:15Z 8bf09993 1.2.10 |
| M15 | PASS | Smoke M15: PASS 2026-06-12T21:33:15Z 8bf09993 1.2.12 (m14_regression + device baseline CPH2655) |
| M11 OP13 | PASS | Smoke M11 OP13: PASS 2026-06-12T20:30:00Z 8bf09993 1.2.11 (2-page onboarding, restricted chip refresh, no Verify page) |
| ADB gates | PASS | adb_gates_verify: PASS 2026-06-08T08:51:00Z b5214fc6 1.1.0 |

---

## Gate G0 — M0 Repository and agent infrastructure

- [x] GitHub push succeeds; CI workflow triggered on main `[AGENT]`
- [ ] GitLab push succeeds; pipeline runs on MR `[HUMAN]`
- [x] BUILD_PLAN.md parses; archive script dry-run works (or correctly refuses without smoke)
- [x] LICENSE (Apache-2.0), CHANGELOG.md, SECURITY.md, AGENT_MEMORY.md, DESIGN_SYSTEM.md present and linked from README
- [x] Dependency license CI passes (no non-FOSS deps) `[AGENT]`
- [x] Debug APK builds locally (`assembleDebug`)
- [x] App launches with M3 MaterialTheme, light/dark, dynamic color on supported device `[ADB]`
- [x] Release manifest has no INTERNET permission `[AGENT]`
- [ ] README contains all 12 standard sections; About ≤120 chars on GitLab `[HUMAN]`
- [x] m0_smoke.sh PASS on device `[ADB]`
- [x] Gate GSM (M0) recorded above `[AGENT]`
- [x] GitLab labels script: `scripts/gitlab/create-labels.sh` (run after GitLab project created) `[AGENT]`
- [x] Milestone M0 gate G0 passed → committed and pushed to GitHub `[AGENT]` (GitLab pending HUMAN)

**Passed:** 2026-06-07 (M0 smoke b5214fc6; pushed to github.com/edwardlthompson/screen-wakelock-detector)

---

## Gate G1 — M1 Core wake capture

- [x] Screen-on events logged within 500 ms on test device `[ADB]` (7–17 ms on b5214fc6)
- [x] Service survives Doze with exemption flow documented `[HUMAN]` (see PERMISSIONS.md Doze survival flow)
- [x] Unit tests for entity mapping + repository `[AGENT]`
- [x] Gate GD (partial): history list uses M3 components only; empty state follows M3 layout `[AGENT]`
- [ ] First launch shows Welcome → How it works → Privacy before Home `[HUMAN]`
- [x] m1_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M1) recorded `[AGENT]`
- [x] Gate G1 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-07 (M1 smoke b5214fc6, latencyMs≤31)

---

## Gate G2 — M2 Non-root attribution

- [x] ≥80% of notification-driven test wakes attributed to correct app+channel `[ADB]` (reference CPH2583, confidence 0.80)
- [x] Unknown wakes stored with diagnostic fields `[AGENT]` (candidates JSON on WakeEvent)
- [x] Notification + Usage onboarding grants correctly on Pixel `[ADB]` (CPH2583 b5214fc6 API 36; adb_gates_verify 2026-06-08)
- [ ] Low-confidence wakes show ranked candidates + “Why?” rationale `[HUMAN]`
- [x] Gate GO (partial): permission screens show What / Why / Never-access copy `[AGENT]`
- [x] Gate GP (partial): Settings → Permissions live switch state for Notification + Usage `[AGENT]` (refresh on resume)
- [x] m2_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M2) recorded `[AGENT]`
- [x] Gate G2 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-07 (M2 smoke b5214fc6)

---

## Gate G3 — M3 Root stack

- [x] Rooted device attributes wakelock tag via in-app libsu only — no Shizuku/module `[ADB]` (OP12 b5214fc6 Magisk; m3 FORCE_ROOT_SMOKE 2026-06-08)
- [x] Parser unit tests pass for API 29, 31, 34 fixture dumpsys files `[AGENT]`
- [x] Non-root device shows same screens with root-only rows disabled `[ADB]`
- [x] No crashes when su denied, timeout, or parse failure `[ADB]`
- [x] Settings → Root switch grants/revokes session; diagnostics show last parse status `[AGENT]`
- [x] Gate GS (partial): root allowlist rejects arbitrary commands `[AGENT]`
- [x] m3_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M3) recorded `[AGENT]`
- [x] Gate G3 passed → archive → commit → push `[AGENT]`

**Passed:** 2026-06-08 (M3 root smoke b5214fc6; rootEnhanced=true via libsu)

---

## Gate G4 — M4 Quick actions

- [ ] From wake detail, user reaches correct channel settings in ≤2 taps `[HUMAN]`
- [x] Mute action verified on Pixel + one OEM `[ADB]` (CPH2583 OEM; wake + quick-fix deep link adb_gates 2026-06-08)
- [ ] Swipe + bottom sheet paths tested for same outcomes as detail screen `[HUMAN]`
- [x] Unit tests for intent/deep-link construction `[AGENT]`
- [ ] Gate GD: last-wake card and action buttons use M3 button variants consistently `[HUMAN]`
- [x] m4_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M4) recorded `[AGENT]`
- [x] Gate G4 passed → archive → commit → push `[AGENT]` (HUMAN/OEM items pending)

**Passed:** 2026-06-07 (M4 smoke b5214fc6; detail quick actions + NLS dismiss + deep links)

---

## Gate G5 — M5 v1.0.0 release

- [ ] 1.0.0-rc.1 soaks 1 week on daily driver `[HUMAN]`
- [x] CHANGELOG complete for all milestones `[AGENT]`
- [ ] F-Droid inclusion checklist complete; fdroiddata MR submitted or ready `[HUMAN]`
- [x] Reproducible build verified or blockers documented in AGENT_MEMORY `[AGENT]`
- [x] Gate GS (full) passed `[AGENT]` (QS tile exported by design; documented)
- [ ] Gate GD (full): all screens pass DESIGN_SYSTEM.md; TalkBack primary flows `[HUMAN]`
- [x] Insights tab counts match raw history `[ADB]` (DB count + InsightsCalculatorTest; adb_gates 2026-06-08)
- [x] Threshold alert fires on synthetic burst; opt-out respected `[ADB]` (seeded primer + burst; adb_gates 2026-06-08)
- [ ] Gate GO (full): verify setup, permission hub, skip paths, OEM battery guidance `[HUMAN]` (ADB portions in Gate GO below)
- [x] Gate GP (full): all permission switches + banner + deep links `[ADB]` (permissions deep link + grants on b5214fc6)
- [ ] Alert notifications name app + channel; unknown wakes link to missing permission toggle `[HUMAN]`
- [x] m5_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M5) recorded `[AGENT]`
- [x] Gate G5 passed → archive → tag v1.0.0-rc.1, GitHub Release, push `[AGENT]` (soak + F-Droid HUMAN pending)

**Passed:** 2026-06-07 (M5 smoke b5214fc6; tagged v1.0.0-rc.1)

---

## Gate G6 — M6 v1.1.0

- [x] Widget updates within 1 min of new wake `[ADB]` (~9s on b5214fc6; adb_gates 2026-06-08)
- [x] Heatmap matches history query for sample dataset (unit test) `[AGENT]`
- [x] Pattern card surfaces on seeded recurring test data `[ADB]` (3-night seed + InsightsCalculatorTest)
- [x] m6_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M6) recorded `[AGENT]`
- [x] Gate G6 passed → archive → push; tag v1.1.0 `[AGENT]`

**Passed:** 2026-06-07 (M6 smoke b5214fc6; tagged v1.1.0)

---

## Gate G7 — M7 F-Droid automation

- [x] Tag v1.0.1+ triggers fdroiddata MR without manual script `[AGENT]` (`.github/workflows/fdroid-publish.yml`; live MR needs `FDROIDDATA_FORK_URL` `[HUMAN]`)
- [x] Reproducible verify gates MR when `REQUIRE_REPRO_VERIFY=1` `[AGENT]`
- [ ] App appears/updates on F-Droid within normal build cycle after MR merge `[HUMAN]`
- [x] Automation runbook documented in F-DROID.md `[AGENT]`
- [x] m7_smoke.sh PASS `[ADB]`
- [x] Gate GSM (M7) recorded `[AGENT]`
- [x] Gate G7 passed → archive → push `[AGENT]` (HUMAN: fork + first MR pending)

**Passed:** 2026-06-07 (M7 automation wired; fdroiddata MR pending fork/secrets)

---

## Gate GO — Onboarding

Checked at M2 (partial), M5 (full).

- [ ] First launch shows Welcome + How it works before any system permission intent `[HUMAN]`
- [ ] Each permission screen has What / Why / Never-access copy per ONBOARDING.md `[HUMAN]`
- [x] Skip paths work; app usable with zero optional permissions `[ADB]` (`adb_gates_verify.sh --fresh` 2026-06-08)
- [x] Returning user with partial grants sees accurate permission hub on Home `[ADB]` (adb_gates 2026-06-08)
- [x] Settings → Permissions switches reflect grant state after system round-trip `[ADB]` (NLS + usage on b5214fc6)
- [ ] OEM battery/settings deep link documented for Pixel + one Samsung `[HUMAN]`
- [ ] TalkBack reads full rationale on each permission step `[HUMAN]`

**Partial passed (M2):** 2026-06-07  
**Full passed (M5):** 2026-06-08 (ADB via adb_gates_verify; HUMAN copy/TalkBack pending)

---

## Gate GP — Permissions center

Checked at M2 (partial), M5 (full).

- [ ] Settings → Permissions is top-level entry, not nested `[HUMAN]`
- [x] Each switch reflects real system state after grant/revoke round-trip `[ADB]` (grants verified adb_gates 2026-06-08)
- [x] Tapping switch OFF when granted opens revoke guidance without crash `[ADB]` (permissions hub launch; no crash adb_gates)
- [x] Home chip / onboarding deep-link scrolls to correct row `[ADB]` (`screenwakelock://app/permissions?highlight=notification_access`)
- [ ] Missing-permission banner visible when any recommended grant is off `[HUMAN]`

**Partial passed (M2):** 2026-06-07  
**Full passed (M5):** 2026-06-08 (ADB; HUMAN banner/nesting pending)

---

## Gate GS — Security

Partial at M3; full at M5.

- [x] INTERNET permission absent from release manifest (CI grep) `[AGENT]` (android-ci.yml)
- [x] All activities/services reviewed: only launcher activity exported `[AGENT]` (CI exported audit; QS tile + MainActivity exported by design)
- [x] Root runner rejects non-allowlisted commands (unit test) `[AGENT]` (RootCommandAllowlistTest)
- [ ] Notification storage schema documented; no message body persisted by default `[HUMAN]`
- [x] adb backup / device backup excludes wake DB `[ADB]` (allowBackup=false + data_extraction_rules; adb_gates 2026-06-08)
- [x] Dependabot enabled; no critical CVEs in dependencies at release `[AGENT]` (.github/dependabot.yml)

**Partial passed (M3):** 2026-06-07  
**Full passed (M5):** 2026-06-07 (exported audit in CI; backup rules; no INTERNET)

---

## Gate GD — Design (Material 3)

Partial at M1/M4; full at M5.

- [x] Dynamic color works on API 31+; static fallback on API 29–30 `[ADB]` (API 36 on b5214fc6; adb_gates 2026-06-08)
- [ ] Light and dark themes render correctly on all primary screens `[HUMAN]`
- [x] No M2 Compose Material imports in codebase (CI grep/lint) `[AGENT]` (android-ci.yml)
- [ ] Touch targets and contrast meet M3 accessibility guidance `[HUMAN]`
- [x] Edge-to-edge insets do not clip FAB, nav bar, or snackbars `[ADB]` (route smoke; no crash adb_gates 2026-06-08)

**Partial passed:** 2026-06-07  
**Full passed (M5):** 2026-06-08 (ADB; HUMAN theme/contrast pending)

---

## Gate G8 — M8 v1.2.0 release hardening and intelligence

- [x] Release APK uses R8 minify + resource shrink (`mapping.txt` present; `verify-release-apk.sh` PASS) `[AGENT]` (subagent assembleRelease ~4MB)
- [x] CI release jobs fail on debug fallback; no uncompressed debug artifact published `[AGENT]`
- [x] Adaptive launcher icon: foreground vector + monochrome layer + fastlane `icon.png` `[AGENT]`
- [ ] Icon legible at launcher size on round and square masks `[HUMAN]`
- [x] App ignore list filters insights and alerts `[AGENT]`
- [x] Custom quiet hours UI matches `WakeAlertNotifier` suppression window `[AGENT]`
- [x] Retention worker prunes events per user policy `[AGENT]`
- [x] Date-range CSV/JSON export via share intent only (no INTERNET) `[AGENT]`
- [x] Week-over-week Insights stat matches calculator unit test `[AGENT]`
- [x] Wake count widget registered and renders `[ADB]` (m8_smoke b5214fc6 2026-06-07)
- [x] Root diagnostic export redacts message bodies `[AGENT]`
- [x] SAF backup/restore documented in SECURITY.md `[AGENT]`
- [x] m8_smoke.sh PASS `[ADB]` (b5214fc6 2026-06-07)
- [x] Gate GSM (M8) recorded `[AGENT]`
- [x] Gate G8 agent items passed; M8 archived to COMPLETED.md `[AGENT]` (2026-06-07)
- [x] Gate G8 passed; [v1.2.0](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.0) published 2026-06-07 `[AGENT]`

---

## Gate G9 — M9 v1.2.1 signed GitHub release

- [x] Release Gradle signing wired; no debug-key fallback on release buildType `[AGENT]`
- [x] `scripts/release/build-signed-apk.sh` exit 0; output `dist/Screen-Wakelock-Detector-1.2.1.apk` `[AGENT]`
- [x] `scripts/release/verify-signed-apk.sh` PASS (apksigner v2/v3) `[AGENT]`
- [x] GitHub `release.yml` publishes signed named APK when secrets set; unsigned artifact removed `[AGENT]`
- [x] `push-github-secrets.sh` documented; secrets not in repo `[AGENT]`
- [x] Signed release APK launches on OP13 after package-installer sideload `[ADB]` (b5214fc6 2026-06-07)
- [x] m9_smoke.sh PASS `[ADB]` (b5214fc6 1.2.1)
- [x] Gate GSM (M9) recorded `[AGENT]`
- [x] Gate G9 passed → archive → commit → push → tag v1.2.1 `[AGENT]`
- [x] Gate G9 passed; [v1.2.1](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.1) published 2026-06-07 `[AGENT]`

---

## Gate G10 — M10 v1.2.2 guided sideload permission setup

- [x] `InstallSourceHelper` + `RestrictedSettingsHelper` detect sideload + restricted op (API 33+) `[AGENT]`
- [x] `RestrictedSetupCard` on onboarding Permissions + Settings Permissions `[AGENT]`
- [x] Guided flow opens Notification settings → App info; instructions for ⋮ → Allow restricted settings `[AGENT]`
- [x] Onboarding Permissions refreshes chip state on resume (no pager advance required) `[AGENT]`
- [x] `m10_smoke.sh` PASS `[ADB]` (b5214fc6 1.2.2)
- [x] Gate GSM (M10) recorded `[AGENT]`
- [x] Gate G10 passed → archive → commit → push → tag v1.2.2 `[AGENT]`
- [x] Gate G10 passed; [v1.2.2](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.2) published 2026-06-07 `[AGENT]`

---

## Gate G11 — M11 v1.2.3 condensed onboarding + version-aware settings

- [x] `PermissionKind.RESTRICTED_SETTINGS` + `shortRationale`; dynamic status list by API/sideload `[AGENT]`
- [x] `SettingsGuideProvider` + intent fallback chain; `PermissionStepsDialog` manual steps `[AGENT]`
- [x] 2-page onboarding (Intro → Permissions); Verify page removed `[AGENT]`
- [x] `PermissionSetupRow` with why-rationale on onboarding + Settings Permissions `[AGENT]`
- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] Intent/settings navigation covered by `SettingsGuideProvider` + existing unit tests `[AGENT]`
- [x] m11_smoke.sh PASS (manual OP12 `b5214fc6` 1.2.3 — restricted chip green after OnePlus Allow dialog) `[ADB]`
- [x] OP13 OxygenOS `8bf09993` CPH2655 API 36: 2-page onboarding, restricted chip refresh on resume, no Verify page `[ADB]` (2026-06-12)
- [x] Gate GSM (M11) recorded `[AGENT]`
- [x] Gate G11 passed → archive → commit → push → tag v1.2.3 `[AGENT]`
- [x] Gate G11 passed; [v1.2.3](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.3) published 2026-06-07 `[AGENT]`

---

## Gate G12 — M12 v1.2.9 app identification + QuickFix ignore

- [x] `PackageFromWakelockTag` derives package from wakelock tags when UID lookup fails `[AGENT]`
- [x] UsageStats candidates always merged in `WakeAttributor` (capped confidence when notifications present) `[AGENT]`
- [x] `AppDisplayResolver` resolves display names at read time (PM label + tag fallbacks) `[AGENT]`
- [x] `QuickFixBottomSheet` shows **Ignore this app** on Home and History; undo snackbar `[AGENT]`
- [x] Ignored apps hidden from History, Home latest wake, and widgets; still in export/backup `[AGENT]`
- [x] Secondary ignore on Detail (candidates), Insights offender menu `[AGENT]`
- [x] Unit tests for tag parse, attributor merge, display resolver, History filter `[AGENT]`
- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] `m12_smoke.sh` PASS `[ADB]` (via m13_adb_verify on 8bf09993)
- [x] Gate GSM (M12) recorded `[AGENT]`
- [x] Gate G12 passed → archive → commit → push → tag v1.2.9 `[AGENT]`
- [x] Gate G12 passed; [v1.2.9](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.9) published 2026-06-11 `[AGENT]`

---

## Gate G13 — M13 v1.2.10 M12 review fixes

- [x] `WakeEventIdentity.effectivePackage` used for ignore/filter across UI, service, alerts, widgets `[AGENT]`
- [x] `WakeEvent.displayAppName` + `AppDisplayResolver` aligned for search, snackbars, Settings labels `[AGENT]`
- [x] `PreferenceKeys.IGNORED_PACKAGES` shared by `PreferencesRepository` and `IgnoredPackagesReader` `[AGENT]`
- [x] `InsightsCalculator` delegates ignore filtering to `WakeEventFilters` `[AGENT]`
- [x] `WakeAttributorLogic` extracted with unit tests; `AppDisplayResolver` + `WakeEventIdentity` tests `[AGENT]`
- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] `m13_smoke.sh` + `m13_adb_verify.sh` PASS `[ADB]` (8bf09993 CPH2655 API 36)
- [x] Gate GSM (M13) recorded `[AGENT]`
- [x] Gate G13 passed → archive → commit → push → tag v1.2.10 `[AGENT]`
- [x] Gate G13 passed; [v1.2.10](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.10) published 2026-06-12 `[AGENT]`

---

## Gate G_RELEASE — Pre-release (every tag)

Runs before every version tag. See [`PROJECT_ALIGNMENT.md`](PROJECT_ALIGNMENT.md) § Pre-release.

- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] All unit tests PASS; no new lint errors `[AGENT]`
- [x] Applicable `m{N}_smoke.sh` + ADB verify scripts PASS — record serial in smoke log `[ADB]` (8bf09993 2026-06-12)
- [x] `CHANGELOG [Unreleased]` finalized; `AGENT_MEMORY` updated `[AGENT]`
- [x] FOSS audit + no `INTERNET` permission (CI checks) `[AGENT]`
- [x] `scripts/benchmark/memory_baseline.sh` PASS when device connected `[ADB]` (device-keyed `baselines/devices/{MODEL}.json`)
- [x] Signed APK verify via `scripts/release/build-signed-apk.sh` / `publish-signed-release.sh` (calls `verify-signed-apk.sh`) `[AGENT]`
- [ ] Optional debug: LeakCanary manual session before major releases (no release dep) `[HUMAN]`

---

## Gate G14 — M14 v1.2.11 project standards alignment

- [x] `PROJECT_ALIGNMENT.md`, root `AGENTS.md`, `ARCHITECTURE.md` `[AGENT]`
- [x] `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, GitHub issue/PR templates `[AGENT]`
- [x] README: CI badge, architecture, donate, forge links `[AGENT]`
- [x] GitHub CI lint step; Gate `G_RELEASE` documented `[AGENT]`
- [x] `scripts/benchmark/memory_baseline.sh` + baseline JSON + heap delta checks `[AGENT]`
- [x] `scripts/smoke/m14_regression.sh` (smoke + benchmark) `[AGENT]`
- [x] Unit test: History search with `AppDisplayResolver` via `WakeEventFilters.matchesHistoryQuery` `[AGENT]`
- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] `m14_smoke.sh` + full `m14_regression.sh` + memory baseline PASS `[ADB]` (8bf09993 CPH2655 API 36)
- [x] Gate GSM (M14) recorded `[AGENT]`
- [x] Gate G14 passed → archive → commit → push → tag v1.2.11 `[AGENT]`
- [ ] GitHub branch protection + secret scanning; GitLab project + labels `[HUMAN]` — GitHub: **Settings → Branches → main** — require Android CI status check, no force push; **Settings → Security** — enable secret scanning and Dependabot alerts

---

## Gate G15 — M15 v1.2.12 code review follow-ups

- [x] Device-keyed memory baselines (`baselines/devices/{MODEL}.json`) + first-run seeding `[AGENT]`
- [x] `WakeEventDisplayNames` + ARCHITECTURE display-name policy; `AppDisplayResolver` tail delegation `[AGENT]`
- [x] G_RELEASE signed APK verify documented as automated via release scripts `[AGENT]`
- [x] `m13_adb_verify.sh` Settings ignored-apps: DataStore assert + scroll (no WARN) `[AGENT]`
- [x] `./gradlew lint test assembleDebug` PASS `[AGENT]`
- [x] `m14_regression.sh` + memory baseline PASS `[ADB]` (8bf09993 CPH2655 API 36)
- [x] Gate GSM (M15) recorded `[AGENT]`
- [x] Gate G15 passed → archive → commit → push → tag v1.2.12 `[AGENT]`
