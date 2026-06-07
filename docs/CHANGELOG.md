# Changelog



All notable changes to **Screen Wakelock Detector** are documented in this file.



The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),

and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

### Added

- Settings About section: version, bundled release notes, GitHub changelog link, Venmo donate link
- `scripts/smoke/grant-magisk-su.py` — Magisk su grant for smoke automation
- Debug smoke deep link `screenwakelock://settings/root?automation=enable` (debug builds only)

### Changed

- M3 root smoke on OP12: libsu attribution, `rootEnhanced=true` in WakeMonitor logs
- `docs/GATES.md` — rooted wakelock gate closed (b5214fc6, 2026-06-08)
- `docs/BUILD_PLAN.md` — HUMAN-only items remain

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

