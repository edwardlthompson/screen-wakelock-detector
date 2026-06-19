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
| **Bootstrap template** | agent-project-bootstrap **0.11.0** (TM sprint 2026-06-19) |
| **material3** | 1.4.0+ stable; expressive opt-in on dedicated branch only |
| **JDK (CI/local)** | 17 (Temurin) |
| **Version source** | `app/build.gradle.kts` → `versionName` / `versionCode` |

---

## CI/CD

| Item | Status |
|------|--------|
| **GitHub branch protection** | `main` — required check **Android CI** (`validate-and-build`); no force push (HUMAN 2026-06-19) |
| **GitHub security** | Secret scanning + push protection + Dependabot security updates enabled (public repo) |
| **GitHub About** | Description + 10 topics via `gh repo edit` / `docs/GITHUB_ABOUT.md` (2026-06-19) |
| **setup-github-repo.sh** | Idempotent security + branch protection (`Android CI` required on `main`) |
| **GitLab pipeline** | _(pending first push)_ |
| **Last pipeline URL** | _(none)_ |
| **Last failed job** | _(none)_ |
| **fdroid-build status** | _(not run)_ |
| **reproducible-verify result** | Stamp file `.fdroid-repro-verified` written on PASS when F-Droid APK available; blocked MR when `REQUIRE_REPRO_VERIFY=1` |
| **Release signing** | Local: `%USERPROFILE%\.screen-wakelock-detector\release.jks` + `keystore.properties`; CI: GitHub secrets via `push-github-secrets.sh`; artifact `dist/Screen-Wakelock-Detector-{version}.apk` |
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

See [`DECISION_LOG.md`](DECISION_LOG.md) for dated architectural decisions.

---

## Device matrix

| Device | Android | OEM | Root | Notes |
|--------|---------|-----|------|-------|
| CPH2583 (OnePlus 12) | 16 | LineageOS | no | TM regression `b5214fc6` wireless |
| CPH2655 (OnePlus 13) | 16 | LineageOS | no | M11–M15 smoke `8bf09993` — **no OxygenOS** |

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
| 2026-06-19 | Audit AU: `docs/REPO_HYGIENE.md` + `docs/PARALLEL_AGENT_SCOPES.md`; README forge/clone → GitHub primary; Android CI green on `95bce34` |
| 2026-06-19 | Bootstrap sync `95bce34`: agent tooling, gates, release automation, living docs at repo root |
| 2026-06-19 | v1.2.13 released: wake attribution (active snapshot, FSI/TURN_SCREEN_ON); tag `v1.2.13` on GitHub |
| 2026-06-19 | Audit AR: `check-github-ci.sh` Android CI auto-detect; `count-critical-high-dependabot.sh`; 0 Critical/High alerts |
| 2026-06-19 | M8 icon legibility automated: `check-icon-legibility.sh` + `m8_icon_launcher.sh` PASS b5214fc6 (round=0.82 safe=0.93 contrast=255) |
| 2026-06-19 | M2/M4/M5 copy/tone automated sign-off: `check-copy-tone.sh` + `m245_copy_verify.sh` PASS b5214fc6 |
| 2026-06-19 | Human backlog automation: `setup-github-repo.sh` + `verify-branch-protection.sh`; GitHub About/topics; F-Droid lint+dry-run PASS; m2/m4/m5 smokes + `m8_icon_launcher.sh` PASS on b5214fc6 (wireless OP12) |
| 2026-06-19 | GitHub security + branch protection: `main` requires **Android CI**; secret scanning, push protection, Dependabot security updates enabled |
| 2026-06-19 | M9 closed: keystore backed up off-device; v1.2.0 GitHub release deprecated (unsigned APK removed, warning in release notes) |
| 2026-06-12 | v1.2.10 published: https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.10 |
| 2026-06-12 | M14 full ADB regression PASS 8bf09993: m14_regression, memory baseline (PSS 127837kB), m13_adb_verify |
| 2026-06-12 | M11 OP13 ADB PASS 8bf09993: 2-page onboarding, restricted chip refresh, no Verify page |
| 2026-06-12 | v1.2.12 published: https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.12 |
| 2026-06-12 | M15 v1.2.12: device-keyed memory baselines, WakeEventDisplayNames, m13 Settings verify hardening |
| 2026-06-11 | v1.2.9 published: https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.9 |
| 2026-06-10 | M12 v1.2.9 implemented: QuickFix ignore, AppDisplayResolver, hide ignored from History/Home/widgets |
| 2026-06-12 | M13 v1.2.10: WakeEventIdentity tag-only ignore, display-name alignment, PreferenceKeys dedup, WakeAttributorLogic tests |
| 2026-06-07 | v1.2.8 published: https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.8 |
| 2026-06-07 | M8 v1.2.0 published: https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.0 |
| 2026-06-07 | M8 ADB deep verify PASS b5214fc6 (root timeline, diagnostic export, pattern actions, batch mute) |

## 2026-06-08 — ADB + agent gate closure

- [x] [AGENT] dependabot.yml for GitHub Actions + Gradle
- [x] [AGENT] GitLab label script (`scripts/gitlab/create-labels.sh`)
- [x] [AGENT] `scripts/smoke/adb_gates_verify.sh` for remaining ADB gate items
- [x] [ADB] Onboarding grants, mute/OEM, insights/threshold, widget, patterns, permissions, backup, dynamic color, edge-to-edge (b5214fc6)
- [x] [ADB] Skip path via `--fresh` (re-grant listener after pm clear)
| 2026-06-07 | M4 quick actions shipped; M3 root stack; M2 attribution; M1 wake capture. |
| 2026-06-06 | M0 documentation and automation scaffold created (docs, scripts, CI templates). |

---

## Design decisions (M13)

- **`WakeEventIdentity`** — single source for effective package (`attributedPackage` ?? tag parse) and ignore checks; used by filters, QuickFix, alerts, service, SilenceWake.
- **`PreferenceKeys.IGNORED_PACKAGES`** — shared DataStore key for `PreferencesRepository` and widget `IgnoredPackagesReader` (no duplicate string key).
- **`WakeAttributorLogic`** — pure helpers (`capUsageCandidateConfidence`, `rootWakeCandidate`) unit-tested without Android mocks.

---

## Risk register (summary)

See plan risk register. Key items:

1. OEM variance for mute/settings intents
2. FOSS compliance on every new Gradle dependency
3. Notification listener + optional root attack surface — see `SECURITY.md`
4. Root command allowlist only — never interpolate user input into shell
5. dumpsys drift — maintain fixture library under `app/src/test/resources/root/`
