# Decision Log

Major architectural and process decisions. Living summary tables remain in [`AGENT_MEMORY.md`](AGENT_MEMORY.md).

---

## 2026-07-22 — Wake Shield thrash / thermal rails

- **Never use `KEYCODE_POWER` as L3 fallback** — it toggles and can create wake/sleep storms.
- **Arm cooldown + self-wake before L1–L3** so shield-caused lock/sleep cannot re-enter enforcement.
- **Reuse wake attribution in `ShieldCoordinator`** — avoid a second root `dumpsys` pass per wake.
- Rails: cooldown **10s**, self-wake suppress **5s** (was 5s / 2s).

## 2026-07-21 — M16 Wake Shield

- **Opt-in multi-tier wake firewall** under `wakeshield/`: L0 forensics → L1 notification cancel → L2 Accessibility re-lock → L3 root sleep / `TURN_SCREEN_ON` deny.
- **Policy:** ~1.5s grace; Unknown / non-allowlisted after grace → enforce (not a user blocklist).
- **Separate shield allowlist** from History ignored apps; panic disable on FGS + Settings; Room v4 evidence columns.
- **L3 requires Magisk/KSU `su`** — ADB root alone is insufficient for in-app libsu.

## 2026-06-06 — Foundation

- **minSdk 29** for usage stats and modern notification APIs.
- **F-Droid-only** distribution; no `INTERNET` permission in release manifest.
- **Self-contained root** via libsu; exclude Shizuku and Magisk modules.
- **GitLab CI** for MRs; fdroiddata MR automation on `v*` tags (M7).

## 2026-06-07 — Release & OEM

- M8 release compression: shrinkResources, material-icons-core, `verify-release-apk.sh` in CI.
- M9 v1.2.1: signed GitHub releases via `build-signed-apk.sh` and CI `RELEASE_*` secrets.
- OP13/OP12 restricted-settings unlock paths documented in ONBOARDING; InstallSourceHelper must not trust packageinstaller alone.

## 2026-06-08 — Settings & distribution

- About section: `BuildConfig.VERSION_NAME`, fastlane changelogs at build time; donate via `ACTION_VIEW`.

## 2026-06-19 — Template migration (TM)

- **Production path locked:** `app/` module — do not relocate to `examples/android/`.
- **License:** Apache-2.0 (not template MIT); rules adapted accordingly.
- **Slash commands:** Migrated to `.cursor/commands/` + `batch-commands.mdc` per bootstrap v0.11.0.
- **BUILD_PLAN:** Relocated to repo root; `docs/COMPLETED.md` retained for milestone archive.
- **Design tokens:** SWD uses `app/.../ui/theme/` + `docs/DESIGN_SYSTEM.md`; minimal `design-tokens/` stub for gate compatibility only.
- **Template version sync:** `.template-version` tracks bootstrap upstream; app versioning remains in `app/build.gradle.kts` + Git tags.
- **Wireless ADB serials:** Transport IDs like `192.168.1.2:44487` must be sanitized in filesystem paths; hardware serial `ro.serialno` remains `b5214fc6` for GATES.md.
- **Local JAVA_HOME:** Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr` when not set globally.
- **M10 cancelled (2026-06-19):** No OxygenOS OP13 available — dev devices are OP12 + OP13 on LineageOS only; OxygenOS ⋮/SAI copy verification dropped.
