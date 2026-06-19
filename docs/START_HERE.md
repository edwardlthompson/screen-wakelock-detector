# Start Here — Screen Wakelock Detector

> **Read this file first** every session (human or agent).

## What is this?

Production **FOSS Android app** (`app/` module) aligned with [agent-project-bootstrap](https://github.com/edwardlthompson/agent-project-bootstrap) v0.11.0. Child repo — not a greenfield template clone.

## Read order

1. [`README.md`](../README.md)
2. [`docs/CURSOR_MODES.md`](CURSOR_MODES.md) — pick Ask / Plan / Agent / Debug
3. [`AGENTS.md`](../AGENTS.md) and [`docs/FOR_AGENTS.md`](FOR_AGENTS.md)
4. [`BUILD_PLAN.md`](../BUILD_PLAN.md) — Sequential lane first
5. [`modules/android/MODULE.md`](../modules/android/MODULE.md)
6. [`docs/GATES.md`](GATES.md) when closing a milestone

## Slash commands

Type **`/`** in Cursor Agent chat. Cheat sheet: [`docs/help/BATCH_COMMANDS.md`](help/BATCH_COMMANDS.md).

| Command | Use when |
|---------|----------|
| `/verify` | Before merge — docs, gates, CI |
| `/ship` | Pre-release — prerelease, push, regress |
| `/gates` | Local `./gradlew lint test assembleDebug` |
| `/bootstrap` | Re-run init + setup + gates (existing repo) |

## Production path (locked)

All feature work targets **`app/`** — not `examples/android/`. See [`docs/BOOTSTRAP_TEMPLATE_MAP.md`](BOOTSTRAP_TEMPLATE_MAP.md).

## Milestone protocol

- Smoke before archive: `scripts/smoke/m{N}_smoke.sh` → record in `GATES.md`
- After each `[AGENT]` step: `bash scripts/watch-agent-gates.sh --once --autofix --step <label>`
- Lead agent only commits/pushes per milestone

## Security

No `INTERNET` in release manifest. FOSS audit: [`docs/FOSS.md`](FOSS.md). Report issues: [`SECURITY.md`](../SECURITY.md).
