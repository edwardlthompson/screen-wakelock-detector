# Agent Router — Screen Wakelock Detector

1. **First read:** [`docs/START_HERE.md`](docs/START_HERE.md)
2. **Cursor modes:** [`docs/CURSOR_MODES.md`](docs/CURSOR_MODES.md)
3. **Ops guide:** [`docs/FOR_AGENTS.md`](docs/FOR_AGENTS.md) — milestone autopilot, smoke, git protocol
4. **Task board:** [`BUILD_PLAN.md`](BUILD_PLAN.md) (Sequential before Parallel)
5. **Living memory:** [`AGENT_MEMORY.md`](AGENT_MEMORY.md) at milestone boundaries · decisions → [`DECISION_LOG.md`](DECISION_LOG.md)
6. **Template map:** [`docs/BOOTSTRAP_TEMPLATE_MAP.md`](docs/BOOTSTRAP_TEMPLATE_MAP.md)

> Legacy `.cursorrules` deprecated. Use `.cursor/rules/*.mdc` and this file.

## Stack

**Android** — production module `app/`. Module guide: [`modules/android/MODULE.md`](modules/android/MODULE.md).

## Session protocol

- Pick Cursor mode per `docs/CURSOR_MODES.md`
- Execute BUILD_PLAN **Sequential** lane first; parallelize `<!-- PARALLEL -->` blocks
- After each `[AGENT]` step: `bash scripts/watch-agent-gates.sh --once --autofix --step <label>`
- Slash commands: [`docs/help/BATCH_COMMANDS.md`](docs/help/BATCH_COMMANDS.md) — `/verify`, `/ship`, `/gates`

## Architecture constraints

- Apache-2.0 FOSS; F-Droid only; **no `INTERNET`** in release manifest
- Material Design 3 (`androidx.compose.material3` only)
- libsu in-app root; command allowlist — see [`docs/ROOT.md`](docs/ROOT.md)

## Module activation

Android only — see `modules/android/MODULE.md`.
