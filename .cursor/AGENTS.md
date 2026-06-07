# Agent orchestration — Screen Wakelock Detector

**Read this file first every session.** Cross-reference: [`.cursor/rules/project.mdc`](rules/project.mdc) · [`docs/BUILD_PLAN.md`](../docs/BUILD_PLAN.md) · [`docs/GATES.md`](../docs/GATES.md)

---

## 1. Parallelism policy

The lead agent **must spawn parallel subagents by default** via the Task tool — **multiple Task calls in one message**.

- Follow `<!-- PARALLEL -->` … `<!-- END PARALLEL -->` blocks in BUILD_PLAN
- Default parallel tracks: feature code ∥ docs/CI ∥ tests when independent
- Label `[PARALLEL-OK]` tasks in the same block may run concurrently
- **Conflict rule:** only the **lead agent** commits and pushes per milestone

| When | Parallel track A | Parallel track B |
|------|------------------|------------------|
| Most milestones | Android feature `[AGENT]` | Docs / BUILD_PLAN / CHANGELOG `[AGENT]` |
| M2 | NotificationListenerService | Permission onboarding UI |
| M3 | Root parsers | Root settings UI |
| M0 | Scaffold | `.gitlab-ci.yml` ∥ fdroid scripts ∥ docs |

Do not wait for the user to request parallelism.

---

## 2. Run-until-milestone-complete

Do **not** stop mid-milestone after one task.

### Autopilot loop

```text
1. Read BUILD_PLAN for current milestone
2. Spawn parallel subagents for PARALLEL blocks
3. Finish ALL [AGENT] tasks in milestone scope
4. Run ./gradlew lint test assembleDebug (when app exists)
5. Run gate checklist (G0–G7, GO, GP, GS, GD, GSM as applicable)
6. Run scripts/smoke/m{N}_smoke.sh — REQUIRED before archive
7. Defer [HUMAN] items → ONE batched checklist at end
8. Update CHANGELOG [Unreleased], AGENT_MEMORY, GATES.md
9. Run archive-completed-tasks.py (only after smoke PASS)
10. Commit milestone message → push GitLab main
11. Do NOT start M(N+1) until M(N) gate passed AND pushed
```

### Stop conditions

- Milestone gate passed, smoke recorded, pushed to GitLab
- OR batched `[HUMAN]` checklist delivered and smoke blocked (document in AGENT_MEMORY — do not archive)

---

## 3. Label semantics

| Label | Meaning |
|-------|---------|
| `[AGENT]` | Implement autonomously — do not stop for user mid-task |
| `[ADB]` | Needs USB device — run if `adb devices` shows device; else mark blocked and continue other work |
| `[HUMAN]` | Needs human — defer to **end of milestone** in one checklist |
| `[PARALLEL-OK]` | Safe concurrent with other PARALLEL-OK in same block |

---

## 4. Validation — confirm → verify → validate

| Phase | Confirm | Verify | Validate |
|-------|---------|--------|----------|
| Code change | Matches BUILD_PLAN task | Compiles; unit tests | Lints; DESIGN_SYSTEM / SECURITY |
| Permission/root | Copy in ONBOARDING.md | `[ADB]` when labeled | Gate GP/GO |
| UI | M3 components only | Preview optional | Gate GD |
| Docs | AGENT_MEMORY updated | Links resolve | README 12 sections |
| Milestone end | Gate + smoke | CI green | Archive only after smoke; push OK |

Mark `[x]` in BUILD_PLAN only after verify. Never assume — run checks.

---

## 5. Git protocol

- **One lead** commits/pushes per milestone
- Subagents do **not** push
- Commit format:

```text
feat(m2): non-root attribution — gate G2 passed

- NotificationListenerService + correlator
- Permission onboarding + Settings switches
- Gate G2, GO partial, GP partial verified
```

- Do not start next milestone until current gate passed and pushed
- If push breaks CI, fix on same milestone — do not advance

---

## 6. When stuck

1. Try an alternative approach (at least twice)
2. Log blocker in [`docs/AGENT_MEMORY.md`](../docs/AGENT_MEMORY.md) — not chat-only
3. Escalate to user only for `[HUMAN]` gates or unrecoverable external deps

---

## 7. Smoke before archive (mandatory)

**Never** run `scripts/archive-completed-tasks.py` for a milestone until:

```bash
bash scripts/smoke/m{N}_smoke.sh   # exit 0
```

Then record in [`docs/GATES.md`](../docs/GATES.md):

```text
Smoke M{N}: PASS <ISO8601> <device_serial> <versionName>
```

Script **refuses to archive** without matching smoke PASS line.

Exception: M0 scaffold may push if smoke blocked, but M0 tasks **stay in BUILD_PLAN** until smoke runs.

---

## Quick reference

| Doc | Purpose |
|-----|---------|
| BUILD_PLAN | Active tasks |
| COMPLETED | Archived after smoke |
| GATES | Gate + smoke log |
| AGENT_MEMORY | Persistent facts |
| DESIGN_SYSTEM | M3 UI rules |
| ADB_TESTING | Smoke script docs |
| FOSS | Dependency policy |
