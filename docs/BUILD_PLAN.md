# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

---

## M9 — v1.2.1 signed GitHub release

**Target:** `versionName` `1.2.1`, `versionCode` `1002001` in `app/build.gradle.kts`  
**Gate:** G9 in `docs/GATES.md` · Smoke: `scripts/smoke/m9_smoke.sh`

### Phase 1 — Signing infrastructure

<!-- PARALLEL -->
<!-- END PARALLEL -->

### Phase 2 — CI and GitHub automation

<!-- PARALLEL -->
<!-- END PARALLEL -->

### Phase 3 — Version bump, smoke, publish

- [ ] [AGENT] Confirm GitHub Release attaches signed `Screen-Wakelock-Detector-1.2.1.apk` (CI or publish-signed-release.sh)

### M9 — HUMAN (batched, milestone end)

- [ ] [HUMAN] Back up `%USERPROFILE%\.screen-wakelock-detector\release.jks` + passwords off-device (required for future in-place updates)
- [ ] [HUMAN] If `gh auth status` fails, run `gh auth login` once so `push-github-secrets.sh` can run
- [ ] [HUMAN] Optional: deprecate v1.2.0 release note — "Download v1.2.1 signed APK instead"

---

## Remaining work (HUMAN only)

- [ ] [HUMAN] GitLab project, MCP, README About, labels (`scripts/gitlab/create-labels.sh` ready)
- [ ] [HUMAN] M2/M4/M5 copy and design walkthroughs (ONBOARDING.md, DESIGN_SYSTEM.md)
- [ ] [HUMAN] 1.0.0-rc.1 soak; F-Droid first inclusion + `FDROIDDATA_FORK_URL` secret
- [ ] [HUMAN] Tag triggers live fdroiddata MR; app on F-Droid after merge
- [ ] [HUMAN] M8 icon legibility on device launcher — round/square masks, light/dark wallpapers

**M8:** Complete — tag [v1.2.0](https://github.com/edwardlthompson/screen-wakelock-detector/releases/tag/v1.2.0) published 2026-06-07.

See [`GATES.md`](GATES.md) for full checklists.
