# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

---

## Remaining work (HUMAN + blocked ADB)

### Blocked ADB (needs hardware)

- [ ] [ADB] Rooted device: wakelock tag via in-app libsu only — run `FORCE_ROOT_SMOKE=1 ANDROID_SERIAL=<rooted> bash scripts/smoke/m3_smoke.sh` when available

### HUMAN gates (unchanged)

- [ ] [HUMAN] GitLab project, MCP, README About, labels (`scripts/gitlab/create-labels.sh` ready)
- [ ] [HUMAN] M2/M4/M5 copy and design walkthroughs (ONBOARDING.md, DESIGN_SYSTEM.md)
- [ ] [HUMAN] 1.0.0-rc.1 soak; F-Droid first inclusion + `FDROIDDATA_FORK_URL` secret
- [ ] [HUMAN] Tag triggers live fdroiddata MR; app on F-Droid after merge

**ADB gate verification:** `scripts/smoke/adb_gates_verify.sh` PASS on b5214fc6 (2026-06-08). Re-grant notification listener after `--fresh`.

---

## Milestone gates

See [`GATES.md`](GATES.md) for full checklists. M0–M7 agent/ADB items closed except rooted wakelock path.
