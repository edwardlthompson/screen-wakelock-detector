# Screen Wakelock Detector

**Find what wakes your screen — see the app and channel, fix it in one tap.**

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Android CI](https://github.com/edwardlthompson/screen-wakelock-detector/actions/workflows/android-ci.yml/badge.svg)](https://github.com/edwardlthompson/screen-wakelock-detector/actions/workflows/android-ci.yml)
[![minSdk](https://img.shields.io/badge/minSdk-29-green.svg)](app/build.gradle.kts)
[![Material 3](https://img.shields.io/badge/UI-Material%203-purple.svg)](docs/DESIGN_SYSTEM.md)

> F-Droid badge — add when published: `[![F-Droid](https://img.shields.io/f-droid/v/com.screenwakelock.detector.svg)](https://f-droid.org/packages/com.screenwakelock.detector/)`

---

## About

Screen Wakelock Detector logs when your phone screen turns on and shows which app or notification channel most likely caused it. Fix unwanted wakes in one tap—open that app's settings or mute the channel—with everything stored locally on your device.

Mystery screen wakes drain battery and interrupt sleep. This app maintains a searchable history of wake events, attributes each wake using notification metadata, usage hints, and optional root diagnostics, and gives you direct links to the settings that control the offender.

**Privacy first:** no account, no cloud, no analytics. Notification listener access reads notification metadata locally to attribute screen wakes — nothing leaves your device. See [`docs/PRIVACY.md`](docs/PRIVACY.md).

**Distribution:** F-Droid only ([build from source](#getting-started) until listed).

**Forges:** [GitLab](https://gitlab.com/) (primary merge requests) · [GitHub](https://github.com/edwardlthompson/screen-wakelock-detector) (CI, releases, issues)

---

## Architecture

Wake events flow: **WakeMonitorService** → **WakeAttributor** → **Room** → Compose UI and Glance widgets. Optional root diagnostics enrich attribution via allowlisted dumpsys parsers.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full diagram and layer breakdown.

---

## Features

- **Wake history** — timestamped log with duration estimates and date grouping
- **Channel attribution** — identify app + notification channel for heads-up and full-screen wakes
- **Confidence scores** — transparent “Why this app?” with ranked candidates when ambiguous
- **Quick actions** — open channel settings, mute channel (where supported), swipe actions
- **Last-wake card** — zero-scroll access to the most recent offender
- **Insights dashboard** — top offenders, nighttime highlights, wake counts (v1.0)
- **Threshold alerts** — optional local notifications naming app + channel
- **Permissions center** — restore missed grants anytime from Settings
- **Optional root** — in-app libsu + dumpsys parsers for wakelock holders (no Shizuku or modules)
- **Widgets & heatmap** — home widget, Quick Settings tile, pattern detection (v1.1)
- **Material Design 3** — dynamic color, light/dark, accessible touch targets

---

## Screenshots

| Light | Dark | Dynamic color |
|-------|------|---------------|
| _Add to `fastlane/metadata/android/en-US/images/phoneScreenshots/`_ | _pending M5_ | _pending M5_ |

Recommended capture order: see [`docs/ONBOARDING.md`](docs/ONBOARDING.md#f-droid-screenshot-order).

---

## Getting started

### Install (F-Droid)

When published: [F-Droid — com.screenwakelock.detector](https://f-droid.org/packages/com.screenwakelock.detector/)

### Build from source

**Requirements:** JDK 17 (Temurin), Android SDK, Git

```bash
git clone https://gitlab.com/<namespace>/screen-wakelock-detector.git
cd screen-wakelock-detector
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release build (signed keystore required for sideload / GitHub download):

```bash
bash scripts/release/setup-keystore.sh    # once; set RELEASE_KEY_PASSWORD
bash scripts/release/build-signed-apk.sh  # → dist/Screen-Wakelock-Detector-{version}.apk
```

Or configure `keystore.properties` from [`keystore.properties.example`](keystore.properties.example). CI uses GitHub secrets `RELEASE_STORE_FILE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

See [`docs/F-DROID.md`](docs/F-DROID.md) for reproducible release builds.

---

## Permissions

| Permission | Why | If denied |
|------------|-----|-----------|
| Notification access | Match wakes to app + channel | Most wakes Unknown |
| Usage access | Fallback when no notification match | Lower confidence |
| Foreground service | Reliable background monitoring | Monitoring may fail |
| Post notifications (API 33+) | Optional threshold alerts | Alerts in-app only |
| Battery unrestricted | Fewer missed wakes in Doze | Gaps in history |
| Root (optional) | Wakelock holder diagnostics | Root features grayed out |

Details: [`docs/PERMISSIONS.md`](docs/PERMISSIONS.md) · Onboarding copy: [`docs/ONBOARDING.md`](docs/ONBOARDING.md)

---

## Usage

1. **First launch** — Welcome → How it works → Privacy (no permissions yet)
2. **Grant notification access** — Settings → Permissions or onboarding prompt
3. **Wait for a wake** — lock phone; when screen turns on, event appears in History
4. **Open detail** — see app, channel, confidence, “Why this app?”
5. **Fix it** — Silence channel or Open notification settings from quick-fix sheet

Optional: enable root in Settings → Root on rooted devices for wakelock tags.

Device smoke tests: [`docs/ADB_TESTING.md`](docs/ADB_TESTING.md)

---

## Root (optional)

Root features use an **in-app stack** (libsu + Kotlin parsers). You do **not** install Shizuku, Magisk modules, or companion apps.

- Requires existing root (Magisk, KernelSU, etc.) and one-time `su` grant
- Non-root users see the same UI with root-only rows disabled

Full detail: [`docs/ROOT.md`](docs/ROOT.md)

---

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for human and agent workflows, FOSS policy, and gate checklists.

---

## Support development

If this app has helped you, consider a tip via [Venmo](https://venmo.com/code?user_id=1857304970395648420) (linked from Settings → About). No tracking, no account required in the app.

---

## Security

This app uses notification listener access and optional root. See [`SECURITY.md`](SECURITY.md) for threat model, exported component policy, and vulnerability reporting.

- Metadata-only notification storage by default
- No `INTERNET` permission
- Root command allowlist with unit tests

---

## License

Copyright 2026 Screen Wakelock Detector contributors

Licensed under the **Apache License 2.0** — see [`LICENSE`](LICENSE).

---

## Links

| Resource | Path |
|----------|------|
| Architecture | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Template alignment | [`docs/PROJECT_ALIGNMENT.md`](docs/PROJECT_ALIGNMENT.md) |
| Contributing | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Documentation index | [`docs/`](docs/) |
| Changelog | [`docs/CHANGELOG.md`](docs/CHANGELOG.md) |
| Privacy policy | [`docs/PRIVACY.md`](docs/PRIVACY.md) |
| Design system | [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) |
| F-Droid guide | [`docs/F-DROID.md`](docs/F-DROID.md) |
| GitLab CI setup | [`docs/GITLAB.md`](docs/GITLAB.md) |
| GitLab issues | _https://gitlab.com/\<namespace\>/screen-wakelock-detector/-/issues_ |
