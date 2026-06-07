# Build Plan

Active tasks only. Completed items move to [`COMPLETED.md`](COMPLETED.md) via `scripts/archive-completed-tasks.py` **after** milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

**Labels:** `[AGENT]` = implement autonomously · `[ADB]` = needs USB device · `[HUMAN]` = defer to milestone-end checklist · `[PARALLEL-OK]` = safe in same `<!-- PARALLEL -->` block

**Parallel maps:** M0 scaffold ∥ docs ∥ CI ∥ fdroid scripts ∥ AGENTS.md · M1 Room+service ∥ M3 theme ∥ onboarding · M2 NLS+cache ∥ permission UI ∥ Settings Permissions · M3 libsu+parsers ∥ Root UI ∥ root onboarding · M4 quick-fix ∥ swipe ∥ WakeAlertNotifier · M5 insights ∥ alerts ∥ onboarding polish ∥ F-Droid · M6 widget ∥ QS tile ∥ heatmap ∥ patterns

---

## M0 — Repository and agent infrastructure

<!-- PARALLEL: spawn subagents for all lines below in one turn -->
<!-- END PARALLEL -->

<!-- PARALLEL -->
- [ ] [AGENT] Create GitLab labels: AGENT, ADB, HUMAN, fdroid, milestone-M0…M7, gate-blocked
- [ ] [HUMAN] Create GitLab public project + fork fdroiddata; configure CI variables (see GITLAB.md)
- [ ] [HUMAN] Connect GitLab MCP in Cursor when ready
<!-- END PARALLEL -->

**Gate G0** — see [`GATES.md`](GATES.md)

---

## M1 — Core wake capture and persistence

<!-- PARALLEL -->
<!-- END PARALLEL -->

<!-- SEQUENTIAL -->
<!-- END SEQUENTIAL -->

<!-- PARALLEL -->
<!-- END PARALLEL -->

**Gate G1** — see [`GATES.md`](GATES.md)

---

## M2 — Non-root attribution

<!-- PARALLEL -->
<!-- END PARALLEL -->

<!-- SEQUENTIAL -->
<!-- END SEQUENTIAL -->

<!-- PARALLEL -->
- [ ] [ADB] Notification + Usage onboarding grants correctly on Pixel
- [ ] [HUMAN] Confirm onboarding What/Why/Never-access copy; low-confidence candidate UI review
<!-- END PARALLEL -->

**Gate G2, GO (partial), GP (partial)** — see [`GATES.md`](GATES.md)

---

## M3 — Self-contained root stack + capability UI

<!-- PARALLEL -->
<!-- END PARALLEL -->

<!-- SEQUENTIAL -->
<!-- END SEQUENTIAL -->

<!-- PARALLEL -->
- [ ] [ADB] Rooted device: wakelock tag via in-app libsu only (no Shizuku/module)
<!-- END PARALLEL -->

**Gate G3, GS (partial)** — see [`GATES.md`](GATES.md)

---

## M4 — Quick actions and remediation

<!-- PARALLEL -->
<!-- END PARALLEL -->

<!-- SEQUENTIAL -->
<!-- END SEQUENTIAL -->

<!-- PARALLEL -->
- [ ] [HUMAN] Wake detail → channel settings in ≤2 taps
- [ ] [ADB] Mute verified on Pixel + one OEM
- [ ] [HUMAN] Swipe + bottom sheet paths match detail screen outcomes
<!-- END PARALLEL -->

**Gate G4, GD (partial)** — see [`GATES.md`](GATES.md)

---

## M5 — Polish, F-Droid release, security audit (v1.0.0)

<!-- PARALLEL -->
- [ ] [AGENT] [PARALLEL-OK] Insights dashboard tab: top offenders, nighttime highlight, counts
- [ ] [AGENT] [PARALLEL-OK] Threshold alerts (opt-in) + permission-missing callout variants
- [ ] [AGENT] [PARALLEL-OK] Onboarding polish: verify setup, battery row, permission chips, replay wizard
- [ ] [AGENT] [PARALLEL-OK] Search/filter: SearchBar, FilterChip, DatePickerDialog
- [ ] [AGENT] [PARALLEL-OK] fastlane metadata + screenshots; fdroiddata MR prep
- [ ] [AGENT] [PARALLEL-OK] Adaptive navigation for tablets/foldables
<!-- END SEQUENTIAL -->

<!-- PARALLEL -->
- [ ] [HUMAN] 1.0.0-rc.1 soak 1 week on daily driver
- [ ] [AGENT] Reproducible build verify or document blockers in AGENT_MEMORY
- [ ] [HUMAN] F-Droid inclusion checklist; fdroiddata MR submitted
- [ ] [AGENT] Gate GS full: exported components, backup rules, no INTERNET
- [ ] [HUMAN] Gate GD full: all screens design checklist; TalkBack walkthrough
- [ ] [ADB] Insights counts match history; threshold alert on synthetic burst
- [ ] [ADB] Run m5_smoke.sh PASS; record in GATES.md
<!-- END PARALLEL -->

**Gate G5, GO (full), GP (full), GS (full), GD (full)** — see [`GATES.md`](GATES.md)

---

## M6 — Widgets and pattern intelligence (v1.1.0)

<!-- PARALLEL -->
- [ ] [AGENT] [PARALLEL-OK] Glance/App Widget: last wake + Fix it deep link
- [ ] [AGENT] [PARALLEL-OK] Quick Settings tile: pause/resume monitoring
- [ ] [AGENT] [PARALLEL-OK] Pattern detection: recurring same app+channel ≥3 nights
- [ ] [AGENT] [PARALLEL-OK] Time heatmap: 7-day hour×day grid with tap-to-filter
<!-- END PARALLEL -->

<!-- PARALLEL -->
- [ ] [ADB] Widget updates within 1 min of new wake
- [ ] [AGENT] Unit test: heatmap matches history query for sample dataset
- [ ] [ADB] Pattern card on seeded recurring test data
- [ ] [ADB] Run m6_smoke.sh PASS; record in GATES.md
<!-- END PARALLEL -->

**Gate G6** — see [`GATES.md`](GATES.md)

---

## M7 — F-Droid publish automation (ongoing)

- [ ] [AGENT] Wire fdroiddata-mr CI job on v* tags after first manual inclusion merged
- [ ] [AGENT] verify-reproducible.sh gates MR creation on hash match
- [ ] [HUMAN] Tag v1.0.1 triggers fdroiddata MR; app updates on F-Droid after MR merge
- [ ] [AGENT] Document automation runbook in F-DROID.md

**Gate G7** — see [`GATES.md`](GATES.md)
