# Changelog



All notable changes to **Screen Wakelock Detector** are documented in this file.



The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),

and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

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

