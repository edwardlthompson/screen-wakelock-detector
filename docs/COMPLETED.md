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

## 2026-06-07 — M7 (smoke passed)

- [x] [AGENT] Wire fdroiddata-mr CI job on v* tags after first manual inclusion merged
- [x] [AGENT] verify-reproducible.sh gates MR creation on hash match
- [x] [AGENT] Document automation runbook in F-DROID.md
- [x] [ADB] Run m7_smoke.sh PASS; record in GATES.md

## 2026-06-08 — ADB + agent gate closure

- [x] [AGENT] dependabot.yml; GitLab create-labels.sh; adb_gates_verify.sh
- [x] [ADB] adb_gates_verify PASS on b5214fc6 (grants, mute, insights, threshold, widget, patterns, permissions, backup, M3 theme)
- [x] [ADB] Rooted wakelock path — OP12 b5214fc6 Magisk, rootEnhanced=true (2026-06-08)

## 2026-06-07 — M8 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Enable `isShrinkResources = true` on release buildType in `app/build.gradle.kts`
- [x] [AGENT] [PARALLEL-OK] Replace `material-icons-extended` with `material-icons-core` in `app/build.gradle.kts`
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/verify-release-apk.sh` — require release APK, `mapping.txt`, size ceiling, reject debug-only artifact
- [x] [AGENT] [PARALLEL-OK] Wire verify script into `.github/workflows/release.yml`, `fdroid-publish.yml`, and `.gitlab-ci.yml` `assemble-release`
- [x] [AGENT] [PARALLEL-OK] Remove GitLab `assembleRelease || assembleDebug` fallback; drop debug APK from release job artifacts
- [x] [AGENT] [PARALLEL-OK] Add `assembleRelease` + verify step to `.github/workflows/android-ci.yml` on main/PR
- [x] [AGENT] [PARALLEL-OK] Document minify + shrinkResources in `docs/F-DROID.md`; note in `docs/AGENT_MEMORY.md`
- [x] [AGENT] Run `./gradlew assembleRelease`; extend `app/proguard-rules.pro` if Glance/Hilt/Room break at runtime
- [x] [AGENT] [PARALLEL-OK] Generate 512×512 reference PNG via image generation
- [x] [AGENT] [PARALLEL-OK] Save reference to `docs/design/icon-reference-512.png`
- [x] [AGENT] [PARALLEL-OK] Rewrite `app/src/main/res/drawable/ic_launcher_foreground.xml`
- [x] [AGENT] [PARALLEL-OK] Update `app/src/main/res/values/colors.xml` — `ic_launcher_background` → `#004D57`
- [x] [AGENT] [PARALLEL-OK] Add `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- [x] [AGENT] [PARALLEL-OK] Reference monochrome layer from adaptive icon XML
- [x] [AGENT] [PARALLEL-OK] Copy 512 PNG to `fastlane/metadata/android/en-US/images/icon.png`
- [x] [AGENT] [PARALLEL-OK] Update `docs/DESIGN_SYSTEM.md` → Store assets: icon spec + color tokens
- [x] [AGENT] [PARALLEL-OK] App ignore list: DataStore + filter in InsightsCalculator, WakeAlertNotifier, WakeMonitorService
- [x] [AGENT] [PARALLEL-OK] Settings → Data & privacy → Ignored apps; Detail “Ignore this app”
- [x] [AGENT] [PARALLEL-OK] Custom quiet hours UI + WakeAlertNotifier wired to nighttime hour keys
- [x] [AGENT] [PARALLEL-OK] History reason-code filter chips
- [x] [AGENT] [PARALLEL-OK] Permission health score on Home
- [x] [AGENT] Update `docs/NOTIFICATIONS.md` quiet-hours copy for configurable window
- [x] [AGENT] [PARALLEL-OK] Retention policy + RetentionWorker + WakeEventDao DELETE
- [x] [AGENT] [PARALLEL-OK] Settings → Auto-delete old events UI
- [x] [AGENT] [PARALLEL-OK] Export sheet: date range + CSV/JSON via ExportUtils
- [x] [AGENT] [PARALLEL-OK] Short-wake filter + screenOffDurationMs in WakeMonitorService
- [x] [AGENT] [PARALLEL-OK] Detail similar wakes section
- [x] [AGENT] [PARALLEL-OK] Full-screen intent banner on Detail
- [x] [AGENT] Unit tests: retention DAO delete, ExportUtils JSON, short-wake filter logic
- [x] [AGENT] [PARALLEL-OK] Week-over-week comparison in InsightsCalculator + InsightsScreen
- [x] [AGENT] [PARALLEL-OK] Per-app nightly budget + WakeAlertNotifier + Insights UI
- [x] [AGENT] [PARALLEL-OK] Pattern card Mute / Open settings actions
- [x] [AGENT] [PARALLEL-OK] Batch mute from Insights top-offender menu
- [x] [AGENT] [PARALLEL-OK] WakeCountWidget Glance widget + manifest
- [x] [AGENT] Unit test: InsightsCalculator week-over-week
- [x] [AGENT] [PARALLEL-OK] Root timeline on Detail when rootEnhanced
- [x] [AGENT] [PARALLEL-OK] RootDiagnosticExporter + RootScreen share
- [x] [AGENT] [PARALLEL-OK] Parser version footnote on Detail
- [x] [AGENT] [PARALLEL-OK] Monitoring pause schedule + QS tile state
- [x] [AGENT] [PARALLEL-OK] SAF backup/restore via BackupUtils + SECURITY.md
- [x] [AGENT] [PARALLEL-OK] Tablet Insights two-column layout
- [x] [AGENT] [PARALLEL-OK] Add `scripts/smoke/m8_smoke.sh`
- [x] [AGENT] [PARALLEL-OK] Document m8 smoke in `docs/ADB_TESTING.md`
- [x] [AGENT] [PARALLEL-OK] Bump version; fastlane changelog + fdroid metadata
- [x] [AGENT] [PARALLEL-OK] CHANGELOG `[1.2.0]`; update `docs/AGENT_MEMORY.md`
- [x] [AGENT] `./gradlew lint test assembleRelease` + verify script (subagent PASS; local JAVA_HOME unset)

## 2026-06-07 — M9 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Add release `signingConfigs` in `app/build.gradle.kts` — load `keystore.properties` or `RELEASE_*` env; unsigned only when no keystore (no debug fallback)
- [x] [AGENT] [PARALLEL-OK] Add Gradle task `copyNamedReleaseApk` → `dist/Screen-Wakelock-Detector-{versionName}.apk`
- [x] [AGENT] [PARALLEL-OK] Add `keystore.properties.example`; add `dist/` to `.gitignore`
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/setup-keystore.sh` — create `%USERPROFILE%\.screen-wakelock-detector\release.jks` + `keystore.properties`; non-interactive when `RELEASE_KEY_PASSWORD` set
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/verify-signed-apk.sh` — `apksigner verify --verbose`; fail if unsigned
- [x] [AGENT] [PARALLEL-OK] Extend `scripts/release/verify-release-apk.sh` — when `EXPECT_SIGNED=1`, require signed APK via verify-signed-apk.sh
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/build-signed-apk.sh` — setup-keystore → assembleRelease → verify (EXPECT_SIGNED=1) → copyNamedReleaseApk → print SHA-256
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/decode-keystore.sh` — shared CI step: base64 secret → `/tmp/release.jks` + export `RELEASE_*` env
- [x] [AGENT] [PARALLEL-OK] Update `.github/workflows/release.yml` — decode keystore, assembleRelease, EXPECT_SIGNED=1, attach `dist/Screen-Wakelock-Detector-*.apk` + `mapping.txt` (drop unsigned)
- [x] [AGENT] [PARALLEL-OK] Mirror signing in `.github/workflows/fdroid-publish.yml`
- [x] [AGENT] [PARALLEL-OK] Add `.github/workflows/republish-release.yml` — `workflow_dispatch` input `tag`; rebuild signed APK and `gh release upload`
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/push-github-secrets.sh` — `gh secret set` for `RELEASE_STORE_FILE_B64`, passwords, alias (reads local keystore.properties)
- [x] [AGENT] [PARALLEL-OK] Add `scripts/release/publish-signed-release.sh` — build-signed-apk → verify-signed-apk → `gh release upload` → delete `app-release-unsigned.apk` asset if present
- [x] [AGENT] [PARALLEL-OK] Wire `EXPECT_SIGNED=1` into `.gitlab-ci.yml` `assemble-release` when `RELEASE_STORE_FILE` set
- [x] [AGENT] Bump `versionName`/`versionCode`; add `fastlane/.../changelogs/1002001.txt`; update `docs/CHANGELOG.md` `[1.2.1]`
- [x] [AGENT] [PARALLEL-OK] Add `scripts/smoke/m9_smoke.sh` — install `dist/Screen-Wakelock-Detector-*.apk`, launch MainActivity, assert version via `dumpsys package`
- [x] [AGENT] [PARALLEL-OK] Document m9 smoke + release scripts in `docs/ADB_TESTING.md`, `docs/F-DROID.md`, `README.md`
- [x] [AGENT] Run `setup-keystore.sh` (once) then `push-github-secrets.sh` when `gh auth status` OK
- [x] [AGENT] Run `./gradlew lint test assembleDebug assembleRelease`; `build-signed-apk.sh` PASS
- [x] [ADB] Run `m9_smoke.sh` PASS on OP13 (`b5214fc6`); record in GATES.md
- [x] [AGENT] Update `docs/AGENT_MEMORY.md` — signed release asset naming, keystore path, OP13 unsigned root cause
- [x] [AGENT] Gate G9 checklist complete; `archive-completed-tasks.py --milestone M9`; commit; push; tag `v1.2.1`

## 2026-06-07 — M10 (smoke passed)

- [x] [AGENT] [PARALLEL-OK] Add `InstallSourceHelper` — sideload detection via `getInstallSourceInfo()`
- [x] [AGENT] [PARALLEL-OK] Add `RestrictedSettingsHelper` — `android:access_restricted_settings` AppOps check (API 33+)
- [x] [AGENT] [PARALLEL-OK] Add `PermissionSetupGuide` + extend `IntentUtils` for guided unlock flow
- [x] [AGENT] [PARALLEL-OK] Add `usePermissionStatuses()` Compose hook — live refresh on ON_RESUME/ON_START
- [x] [AGENT] [PARALLEL-OK] Fix `OnboardingPermissions` live refresh; add `RestrictedSetupCard` on Permissions + Settings Permissions
- [x] [AGENT] [PARALLEL-OK] Unit tests for install-source allowlist + restricted-settings logic
- [x] [AGENT] Bump version; CHANGELOG `[1.2.2]`; fastlane changelog `1002002.txt`
- [x] [AGENT] Update `docs/PERMISSIONS.md`, `docs/ONBOARDING.md`, `docs/ADB_TESTING.md`, `docs/AGENT_MEMORY.md`
- [x] [AGENT] Add `scripts/smoke/m10_smoke.sh`
- [x] [AGENT] `./gradlew lint test assembleDebug`
- [x] [ADB] Guided flow on OP13 sideload; chips update on Permissions page without pager advance
