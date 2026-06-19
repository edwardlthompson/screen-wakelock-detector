# Changelog



All notable changes to **Screen Wakelock Detector** are documented in this file.



The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),

and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

## [1.2.13] — 2026-06-19

Wake attribution improvements for ongoing notifications and OP13 unknown-wake reduction.

### Added

- Live **active notification snapshot** at wake time (merges with cache window)
- `NOTIFICATION_FULL_SCREEN` / `FLAG_TURN_SCREEN_ON` / alarm category detection
- Usage stats `NOTIFICATION_INTERRUPTION` fallback (API 29+)
- `scripts/smoke/attribution_verify.sh` + `attribution_dual_verify.sh` (OP13 USB + OP12 wireless)
- `scripts/smoke/_unlock.sh` — `SMOKE_PIN` support for UI smokes on locked devices

### Changed

- Notification cache stores **observe time** (listener receive) instead of `sbn.postTime`
- `NotificationCaptureService` logs listener disconnect warnings

### Fixed

- Ongoing alarms/calls/FSI notifications missed when post time fell outside ±5s correlation window
- `m13_adb_verify.sh` — clear ignored apps before M12; longer QuickFix wait; unlock helper

## [1.2.12] — 2026-06-12

Code review follow-ups — device baselines, display-name clarity, smoke hardening.

### Added

- Device-keyed memory baselines (`scripts/benchmark/baselines/devices/{MODEL}.json`) with first-run seeding
- `WakeEventDisplayNames.offlineAppName` — shared offline display-name helper + unit tests
- ARCHITECTURE display-name policy table; memory benchmark section in `ADB_TESTING.md`

### Changed

- `memory_baseline.sh` compares per-device baselines; monotonic history scoped by device
- `m13_adb_verify.sh` — Settings ignored-apps verified via DataStore + scroll (no WARN)
- `AppDisplayResolver` tail delegates to `WakeEventDisplayNames`; `WakeEvent.displayAppName` documented as offline-only
- Gate G_RELEASE: signed APK verify automated via release scripts

## [1.2.11] — 2026-06-12

Project standards alignment — docs, CI parity, memory benchmark, regression smoke.

### Added

- `docs/PROJECT_ALIGNMENT.md`, `docs/ARCHITECTURE.md`, root `AGENTS.md` entrypoint
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, GitHub issue/PR templates
- Gate **G_RELEASE** pre-release checklist in `GATES.md`
- `scripts/benchmark/memory_baseline.sh` with baseline JSON and heap delta checks
- `scripts/smoke/m14_regression.sh` (smoke + memory benchmark + M13 ADB verify)
- `WakeEventFilters.matchesHistoryQuery` for History search with `AppDisplayResolver`
- GitHub CI lint step (parity with GitLab)

### Changed

- README: CI badge, architecture overview, donate link, GitLab/GitHub forge clarity

## [1.2.10] — 2026-06-12

M12 review fixes — tag-only ignore, display-name consistency, test coverage.

### Fixed

- **Ignore this app** and list filtering now use wakelock-tag-derived package when `attributedPackage` is null (tag-only root wakes).
- History search, mute snackbars, and Settings ignored-app labels use `AppDisplayResolver` (matches cards).
- `WakeEvent.displayAppName` extended with wakelock-tag fallbacks aligned to display resolver.
- `InsightsCalculator` delegates ignore filtering to `WakeEventFilters` (single predicate).
- Shared `PreferenceKeys.IGNORED_PACKAGES` for repository and widget reader.

### Added

- `WakeEventIdentity` — effective package + ignore check for events.
- `WakeAttributorLogic` pure helpers with unit tests.
- Unit tests: `WakeEventIdentity`, `AppDisplayResolver`, `WakeAttributorLogic`; extended filter/insights tests.

## [1.2.9] — 2026-06-10

QuickFix ignore, stronger unknown-app identification, hide ignored apps from History.

### Added

- **Ignore this app** on the Home/History **Fix it** popup (`QuickFixBottomSheet`) with undo snackbar.
- **Ignore this app** on Insights top-offender menu and Detail candidate rows.
- `PackageFromWakelockTag` — derive package from root wakelock tags when UID lookup fails.
- `AppDisplayResolver` — display-time app labels with wakelock-tag fallbacks.
- Low-confidence wake cards show wakelock tag hints when the name is still unknown.

### Changed

- UsageStats candidates always merged into attribution (lower confidence when notifications exist).
- Ignored apps hidden from History, Home latest wake, and home-screen widgets (still in export/backup).
- Settings **Ignored apps** copy notes History hiding; **Remove** restores visibility.

## [1.2.8] — 2026-06-07

Fix misleading restricted-settings menu guidance after OP13 smoke.

### Fixed

- Steps now say **More options** (beside **Open** on **App info**), not a generic three-dot menu — the Notification access screen has no overflow menu.
- When Grant opens App info first, step 1 says the screen is already open instead of asking to tap Open App info again.

## [1.2.7] — 2026-06-07

Fix restricted-settings guidance on Android 15/16 (Enhanced Confirmation).

### Fixed

- Android 15+ sideloads: Grant on **Allow restricted settings** now opens **App info** first (⋮ → Allow restricted settings → screen lock), because the notification toggle shows **App was denied access** with no Allow button — only **Learn how to allow access** / Close.
- Updated in-app steps for ECM / Lineage Android 16; Notification access remains the secondary button.

## [1.2.6] — 2026-06-07

Fix restricted-settings navigation on Lineage / sideloaded installs.

### Fixed

- Allow restricted settings: Grant now opens this app's **Notification access** toggle (detail settings intent) so Android shows the restricted-setting dialog; App info is a secondary button for the ⋮ menu fallback.
- Lineage/AOSP steps updated — turn the notification switch On first, then use App info only if the dialog did not appear.

## [1.2.5] — 2026-06-07

Fix startup crash on cold launch.

### Fixed

- Startup crash: `RetentionWorker` and `PreferencesRepository` each opened a separate DataStore for `settings.preferences_pb`; when WorkManager ran before the UI, the app crashed immediately with “multiple DataStores active for the same file” (reproduced on OP13 / Lineage Android 16).

## [1.2.4] — 2026-06-07

Fix sideload detection for GitHub/browser APK installs.

### Fixed

- GitHub/browser APK installs: no longer treat `com.android.packageinstaller` as trusted — Firefox/Chrome/Files downloads correctly show the restricted-settings flow (OP13 Lineage repro).
- LineageOS: restricted-settings steps use App info → ⋮ path instead of OxygenOS-only copy on OnePlus hardware.

## [1.2.3] — 2026-06-07

Condensed onboarding and version-aware permission setup.

### Added

- Two-page onboarding: Intro (welcome + how it works + privacy) → Permissions → Get started.
- `PermissionSetupRow` with brief why-rationale under each permission chip.
- Live **Allow restricted settings** chip when sideloaded (API 33+); replaces separate setup card.
- `SettingsGuideProvider` with API/OEM-aware intent chains and numbered manual steps when Settings cannot open directly.
- `PermissionStepsDialog` with Open Settings retry and generic installer workaround (session install).

### Fixed

- OnePlus/OxygenOS: restricted-settings chip now turns green when Notification or Usage access is granted, even if AppOps stays at `default`.
- OnePlus restricted-settings instructions: use system **Restricted setting** dialog **Allow** first, then App info ⋮ fallback.

### Changed

- Removed Verify onboarding page; Permissions is the final step.
- Alert notifications row hidden below API 33.
- Settings → Permissions uses short rationale; shared settings navigator across onboarding and settings.

### Removed

- `RestrictedSetupCard` (replaced by restricted-settings permission chip + steps dialog).

## [1.2.2] — 2026-06-07

Guided sideload permission setup for Android 13+.

### Added

- Detect sideloaded installs and restricted-settings lock; `RestrictedSetupCard` with step-by-step unlock flow.
- Opens App info for **Allow restricted settings** (⋮ menu); OnePlus SAI fallback instructions.
- Live permission status on onboarding Permissions page (refreshes on return from Settings without advancing pager).
- `usePermissionStatuses()` hook shared across onboarding, Permissions screen, and home banner.

### Fixed

- Onboarding Permissions page no longer freezes permission chip state until Verify page.

## [1.2.1] — 2026-06-07

Signed GitHub release fix.

### Fixed

- GitHub release APK is now **signed** (`Screen-Wakelock-Detector-{version}.apk`); unsigned v1.2.0 artifact failed to launch on some devices (e.g. OnePlus 13).

### Added

- Release signing: `keystore.properties` / CI secrets, `scripts/release/build-signed-apk.sh`, `verify-signed-apk.sh`, `m9_smoke.sh`.
- `republish-release.yml` workflow_dispatch for re-uploading signed assets.

## [1.2.0] — 2026-06-07

M8 release hardening and intelligence.

### Added

- High-contrast adaptive launcher icon (phone + amber screen glow + notification dot).
- App ignore list; custom quiet hours UI; reason-code History filter chips; permission health score on Home.
- Retention policy with daily WorkManager prune; date-range CSV/JSON export sheet; short-wake filter.
- Detail similar wakes section; full-screen intent banner on Detail.
- Week-over-week comparison stat on Insights.
- Per-app nightly wake budgets with alert when exceeded.
- Recurring pattern cards: Mute channel and Open settings actions.
- Batch mute from Insights top-offender menu with OEM fallback messaging.
- Wake count Glance widget: tonight's count + top offender.
- Root parser footnote on Detail when root-enhanced; root timeline for same package.
- Root diagnostic text export via share intent — no message bodies.
- Monitoring pause schedule respected by WakeMonitorService; QS tile shows schedule pause.
- Local JSON backup/restore via SAF (export + import with confirmation).
- Tablet Insights two-column layout at width ≥840dp.
- `scripts/release/verify-release-apk.sh` and `scripts/smoke/m8_smoke.sh`.

### Changed

- Release builds: R8 resource shrinking (`isShrinkResources`); CI verifies minified APK before publish.
- Replaced `material-icons-extended` with `material-icons-core`.
- `RootAttributor` exposes `parserId` on snapshots; stored on wake events as `rootParserId`.
- Version 1.2.0 / versionCode 1002000.

## [1.1.0] — 2026-06-07

Widgets and pattern intelligence release.

### Added

- Glance home-screen widget: last wake summary with **Fix it** deep link to quick-fix sheet (M6).
- Quick Settings tile toggles monitoring pause/resume (M6).
- Recurring pattern cards (≥3 nights same app+channel) in Insights (M6).
- 7-day heatmap with tap-to-filter History by hour (M6).
- Deep links: `screenwakelock://wake/latest`, `screenwakelock://insights/heatmap` (M6).

### Changed

- Widget **Fix it** row opens `wake/latest/actions` instead of generic app launch (M6).

## [1.0.0-rc.1] — 2026-06-07



First release candidate for F-Droid.



### Added



- Insights dashboard: total/nighttime counts, top offenders with channel names, recurring patterns, 7-day heatmap with tap-to-filter history (M5).

- Threshold alert count slider, quiet hours (11pm–6am), and wake alert permission row in Settings (M5).

- Home and detail missing-permission banners with deep link to Permissions (M5).

- History date picker filter and hour filter from Insights heatmap (M5).

- Adaptive navigation: bottom bar on phones, navigation rail on tablets/foldables (M5).

- Onboarding skip paths, verify step refresh on resume, OEM battery guidance (M5).

- Permissions screen “Run setup again” entry (M5).

- fastlane metadata and F-Droid metadata updated for 1.0.0-rc.1 (M5).



### Changed



- Wake alert notifier respects quiet hours for threshold and single-wake alerts (M5).



### Prior milestones (included in rc.1)



- M0: project scaffold, CI, docs, smoke scripts.

- M1: wake capture, Room persistence, history UI, onboarding shell.

- M2: notification listener attribution, detail screen, permissions center.

- M3: libsu root stack, dumpsys parsers, Root settings UI.

- M4: quick-fix bottom sheet, channel mute via NLS dismiss, deep links.



---



## Release history



- `[1.1.0]` — M6: widget, Quick Settings tile, pattern detection, heatmap polish

- `[1.0.0]` — M5 stable after rc soak

- `[0.9.0-beta.N]` — M4 feature-complete beta

- `[0.5.0-alpha.N]` — M2 attribution alpha

- `[0.1.0-alpha.N]` — M1 early dogfood

