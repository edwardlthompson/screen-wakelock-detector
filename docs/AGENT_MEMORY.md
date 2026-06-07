# Agent Memory

Persistent facts for AI agents working on **Screen Wakelock Detector**. Update this file after significant events, gate passes, CI failures, and design decisions.

---

## Project facts

| Field | Value |
|-------|-------|
| **App name** | Screen Wakelock Detector |
| **Package** | `com.screenwakelock.detector` |
| **License** | Apache-2.0 |
| **Distribution** | F-Droid only (no Google Play) |
| **GitHub repo** | https://github.com/edwardlthompson/screen-wakelock-detector |
| **Primary forge** | GitHub (GitLab mirror pending HUMAN) |
| **GitLab project ID** | _(set after project creation)_ |
| **fdroiddata fork URL** | _(set after fork — e.g. `git@gitlab.com:<user>/fdroiddata.git`)_ |
| **Stack** | Kotlin, Jetpack Compose, Material Design 3, Room, Hilt |
| **Root library** | libsu (`com.github.topjohnwu.libsu:core`) — in-app only, no Shizuku |
| **minSdk** | 29 (Android 10) |
| **targetSdk** | 35 |
| **Modules** | Single `app` module |
| **Compose BOM** | _(pin in `gradle/libs.versions.toml` when scaffold exists)_ |
| **material3** | 1.4.0+ stable; expressive opt-in on dedicated branch only |
| **JDK (CI/local)** | 17 (Temurin) |
| **Version source** | `app/build.gradle.kts` → `versionName` / `versionCode` |

---

## CI/CD

| Item | Status |
|------|--------|
| **GitLab pipeline** | _(pending first push)_ |
| **Last pipeline URL** | _(none)_ |
| **Last failed job** | _(none)_ |
| **fdroid-build status** | _(not run)_ |
| **reproducible-verify result** | Stamp file `.fdroid-repro-verified` written on PASS when F-Droid APK available; blocked MR when `REQUIRE_REPRO_VERIFY=1` |
| **Release signing** | Keystore in CI variables only — never in repo |
| **Release compression (M8)** | `isMinifyEnabled` + `isShrinkResources`; `material-icons-core` (not extended); verify via `scripts/release/verify-release-apk.sh` |

---

## GitLab MCP

| Item | Status |
|------|--------|
| **MCP connected** | _(unknown — ask user)_ |
| **glab authenticated** | _(unknown)_ |
| **Token scopes needed** | `api`, `read_repository`, `write_repository` |
| **fdroiddata MR assignee** | _(optional default)_ |
| **Pipeline URL template** | `https://gitlab.com/<namespace>/screen-wakelock-detector/-/pipelines/<id>` |

---

## Design decisions

| Decision | Status |
|----------|--------|
| **Dynamic color** | Enabled on API 31+; static light/dark fallback on 29–30 |
| **M3 Expressive** | Opt-in for hero surfaces only; never block release on alpha APIs |
| **Material 2 imports** | Forbidden — CI grep in `.gitlab-ci.yml` |
| **Edge-to-edge** | Required per Android 15+ guidance |
| **Theme tokens** | Documented in `docs/DESIGN_SYSTEM.md` |

---

## Decisions log

- **2026-06-06:** minSdk 29 for usage stats and modern notification APIs.
- **2026-06-06:** F-Droid-only distribution; no `INTERNET` permission in release manifest.
- **2026-06-06:** Self-contained root stack via libsu; explicitly exclude Shizuku and Magisk modules.
- **2026-06-06:** GitLab CI replaces GitHub Actions; fdroiddata MR automation on `v*` tags (M7).
- **2026-06-08:** Settings About section: `BuildConfig.VERSION_NAME`, release notes copied from `fastlane/.../changelogs/{versionCode}.txt` at build time, changelog/donate via `ACTION_VIEW` (no `INTERNET` permission). F-Droid `Donate` field set to Venmo URL.
- **2026-06-07:** M8 release compression: shrinkResources, material-icons-core, `verify-release-apk.sh` wired into GitHub/GitLab CI.
- **2026-06-07:** M8 v1.2.0: intelligence features (ignore list, retention, WoW insights, WakeCountWidget, SAF backup). Launcher icon updated. Local `./gradlew` blocked (JAVA_HOME unset on dev machine); CI/subagent verify PASS.

---

## Device matrix

| Device | Android | OEM | Root | Notes |
|--------|---------|-----|------|-------|
| CPH2583 (OnePlus) | 15 | OnePlus | no | M0–M5 smoke device `b5214fc6` |

---

## Known limitations

- OEM variance: Samsung/Xiaomi may restrict channel mute or settings deep links.
- Attribution ambiguity: multiple notifications in correlation window → show ranked candidates.
- `dumpsys` output varies by OEM/Android version — versioned parsers + fallback chain required.
- Some kernels block `/sys/kernel/debug/wakeup_sources` — degrade to `dumpsys power` only.
- Root is optional; never required for core screen-on logging.

---

## Blockers / open questions

- _(none)_

---

## Recent events

| Date | Event |
|------|-------|
| 2026-06-07 | M8 v1.2.0 implemented: compression, icon, phases A–D; smoke PASS b5214fc6; 46 tasks archived. |

## 2026-06-08 — ADB + agent gate closure

- [x] [AGENT] dependabot.yml for GitHub Actions + Gradle
- [x] [AGENT] GitLab label script (`scripts/gitlab/create-labels.sh`)
- [x] [AGENT] `scripts/smoke/adb_gates_verify.sh` for remaining ADB gate items
- [x] [ADB] Onboarding grants, mute/OEM, insights/threshold, widget, patterns, permissions, backup, dynamic color, edge-to-edge (b5214fc6)
- [x] [ADB] Skip path via `--fresh` (re-grant listener after pm clear)
| 2026-06-07 | M4 quick actions shipped; M3 root stack; M2 attribution; M1 wake capture. |
| 2026-06-06 | M0 documentation and automation scaffold created (docs, scripts, CI templates). |

---

## Risk register (summary)

See plan risk register. Key items:

1. OEM variance for mute/settings intents
2. FOSS compliance on every new Gradle dependency
3. Notification listener + optional root attack surface — see `SECURITY.md`
4. Root command allowlist only — never interpolate user input into shell
5. dumpsys drift — maintain fixture library under `app/src/test/resources/root/`
