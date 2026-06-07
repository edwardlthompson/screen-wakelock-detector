# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

---

## Remaining work (HUMAN only)

- [ ] [HUMAN] GitLab project, MCP, README About, labels (`scripts/gitlab/create-labels.sh` ready)
- [ ] [HUMAN] M2/M4/M5 copy and design walkthroughs (ONBOARDING.md, DESIGN_SYSTEM.md)
- [ ] [HUMAN] 1.0.0-rc.1 soak; F-Droid first inclusion + `FDROIDDATA_FORK_URL` secret
- [ ] [HUMAN] Tag triggers live fdroiddata MR; app on F-Droid after merge

**ADB / agent gates:** All closed. Root path verified on OP12 (`b5214fc6`) via `FORCE_ROOT_SMOKE=1 bash scripts/smoke/m3_smoke.sh` (2026-06-08).

See [`GATES.md`](GATES.md) for full checklists.
