# Build Plan

Active tasks only. Completed work: [`docs/COMPLETED.md`](docs/COMPLETED.md), [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md), [`docs/GATES.md`](docs/GATES.md).

**Current sprint:** _(none)_

**Labels:** `[AGENT]` · `[ADB]` · `[HUMAN]` · `[AUTO]` · `[PARALLEL-OK]`

---

## Open (HUMAN)

- [ ] **GitLab** — Create project, connect MCP, paste About from [`docs/GITLAB.md`](docs/GITLAB.md), run `scripts/gitlab/create-labels.sh`
- [ ] **GitHub security** — Enable private vulnerability reporting (Settings → Code security)
- [ ] **F-Droid first ship** — 1-week `1.0.0-rc.1` soak on daily driver; fork fdroiddata; set `FDROIDDATA_FORK_URL` + `GITLAB_TOKEN` secrets so tag pushes open live MRs

Prep already done: metadata lint + `DRY_RUN=1 prepare-fdroiddata-mr.sh` PASS (2026-06-19).

---

## Archived sprints

| Sprint | Archive |
|--------|---------|
| M0–M15 | [`docs/COMPLETED.md`](docs/COMPLETED.md) |
| TM · AR · AU · human backlog automation | [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md) |
