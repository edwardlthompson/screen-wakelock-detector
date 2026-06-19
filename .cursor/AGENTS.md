# Agent orchestration — Screen Wakelock Detector

**Read [`docs/START_HERE.md`](../docs/START_HERE.md) first.** Full ops: [`docs/FOR_AGENTS.md`](../docs/FOR_AGENTS.md).

Cross-reference: [`rules/project.mdc`](rules/project.mdc) · [`BUILD_PLAN.md`](../BUILD_PLAN.md) · [`docs/GATES.md`](../docs/GATES.md)

---

## Parallelism policy

The lead agent **must spawn parallel subagents by default** via the Task tool — **multiple Task calls in one message**.

- Follow `<!-- PARALLEL -->` … `<!-- END PARALLEL -->` blocks in BUILD_PLAN
- Label `[PARALLEL-OK]` tasks in the same block may run concurrently
- **Conflict rule:** only the **lead agent** commits and pushes per milestone

---

## Run-until-milestone-complete

1. Read BUILD_PLAN Sequential lane
2. Spawn parallel subagents for PARALLEL blocks
3. Finish ALL `[AGENT]` tasks in milestone scope
4. Run `./gradlew lint test assembleDebug`
5. Run gate checklist (`docs/GATES.md`)
6. Run `scripts/smoke/m{N}_smoke.sh` — **required** before archive
7. Defer `[HUMAN]` → one batched checklist at end
8. Update CHANGELOG, AGENT_MEMORY, GATES; archive only after smoke PASS
9. Lead agent commit → push

---

## Labels

| Label | Meaning |
|-------|---------|
| `[AGENT]` | Implement autonomously |
| `[ADB]` | Needs USB device |
| `[HUMAN]` | Defer to milestone-end checklist |
| `[PARALLEL-OK]` | Safe concurrent in same PARALLEL block |

---

## Smoke before archive (mandatory)

Never run `scripts/archive-completed-tasks.py` until:

```bash
bash scripts/smoke/m{N}_smoke.sh   # exit 0
```

Record in `docs/GATES.md`: `Smoke M{N}: PASS <ISO8601> <serial> <versionName>`

---

## Quick reference

| Doc | Purpose |
|-----|---------|
| BUILD_PLAN | Active tasks |
| COMPLETED.md | Archived milestones |
| GATES | Gate + smoke log |
| AGENT_MEMORY | Persistent facts |
| BATCH_COMMANDS | Slash command cheat sheet |
