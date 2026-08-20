# Build Plan

Active tasks only. Completed work: [`docs/COMPLETED.md`](docs/COMPLETED.md), [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md), [`docs/GATES.md`](docs/GATES.md).

**Current sprint:** M16 Wake Shield

**Labels:** `[AGENT]` · `[ADB]` · `[HUMAN]` · `[AUTO]` · `[PARALLEL-OK]`

**Status:** 🔲 open · ✅ done · ❌ blocked (emoji only — never GitHub task-list checkboxes).

---

## Sequential — M18 product slices

- ✅ [AGENT] Cut UNKNOWN attribution (tag fallback + low-importance notifs)
- ✅ [AGENT] History chips: Unknown / Shielded / Allowed
- ✅ [AGENT] Local weekly shield digest
- ✅ [AGENT] Finish F-Droid listing pack
- ✅ [AGENT] Backup night-only ignore
- ✅ [AGENT] Home last-wake shield line
- ✅ [AGENT] Insights offender night-ignore / allowlist

## Sequential — M17 product slices

- ✅ [AGENT] Insights: Wake Shield week totals
- ✅ [AGENT] Home unknown-rate chip + grant nudge
- ✅ [AGENT] Quiet-hours / night-only ignore
- ✅ [AGENT] F-Droid phone screenshots
- ✅ [AGENT] Widget: shield armed + last outcome
- ✅ [AGENT] Settings Ignored-apps findability
- ✅ [AGENT] Detail: why this shield decision

## Sequential — M16 Wake Shield

- ✅ [AGENT] Scaffold `wakeshield/` + prefs + Room `shieldOutcome` + `ShieldPolicy` unit tests
- ✅ [AGENT] Safety rails: panic, cooldown, self-wake, appops undo, OEM exempt pack
- ✅ [AGENT] L0 forensics + L1 notification cancel while shield armed
- ✅ [AGENT] L2 Accessibility lock service + Settings capability row
- ✅ [AGENT] L3 `RootWakeEnforcer` + allowlisted sleep/appops/wake_unlock + ROOT.md/tests
- ✅ [AGENT] Settings Wake Shield UI + Detail outcome banner + docs/CHANGELOG
- ✅ [ADB] `scripts/smoke/m16_smoke.sh` on OP13 arm/register PASS (`8bf09993` 1.2.14–1.2.15)
- 🔲 [ADB] OP12 Magisk: alarm allow + hostile sleep + panic (needs `SMOKE_PIN` or unlock)
- 🔲 [HUMAN] Soak: mystery wakes + false nuclear check before archive

---

## Open (HUMAN)

- 🔲 **GitLab** — Create project, connect MCP, paste About from [`docs/GITLAB.md`](docs/GITLAB.md), run `scripts/gitlab/create-labels.sh`
- 🔲 **GitHub security** — Enable private vulnerability reporting (Settings → Code security)
- 🔲 **F-Droid first ship** — 1-week `1.0.0-rc.1` soak on daily driver; fork fdroiddata; set `FDROIDDATA_FORK_URL` + `GITLAB_TOKEN` secrets so tag pushes open live MRs

Prep already done: metadata lint + `DRY_RUN=1 prepare-fdroiddata-mr.sh` PASS (2026-06-19). v1.2.15 released 2026-07-22.

---

## Archived sprints

| Sprint | Archive |
|--------|---------|
| M0–M15 | [`docs/COMPLETED.md`](docs/COMPLETED.md) |
| TM · AR · AU · ATTR · human backlog automation | [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md) |
