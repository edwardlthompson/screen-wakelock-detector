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

- Apache-2.0 FOSS; F-Droid only; `INTERNET` only for GitHub latest-release checks
- Material Design 3 (`androidx.compose.material3` only)
- libsu in-app root; command allowlist — see [`docs/ROOT.md`](docs/ROOT.md)

## Module activation

Android only — see `modules/android/MODULE.md`.

## Cursor FOSS integrations

Shipped from bootstrap `v0.21.0` (see `docs/CURSOR_INTEGRATIONS.md`):

- **Hooks** — `.cursor/hooks.json` (destructive-ops + UTF-8; fail-open)
- **Skills** — `.cursor/skills/` companions for `/gates`, `/scope`, `/fix`, hygiene
- **Subagents** — `.cursor/agents/` verifier, gate-fixer, explorer
- **Local compute first** — `.cursor/rules/local-compute.mdc`
- **Worktrees** — `.cursor/worktrees.json` + setup scripts
- **Optional MCP** — copy `.cursor/mcp.foss.example` → gitignored `.cursor/mcp.json`

Validate: `python3 scripts/agent-run.py check-cursor-hooks -- --smoke` (if `agent-run.py` is available).

Commercial Cursor surfaces are not activated (`distribution_tier: foss`).

## This child repo

- **Stack:** Android only — production `app/`; do not reintroduce web/python examples
- **License:** Apache-2.0 (not template MIT)
- **Alignment:** `docs/BOOTSTRAP_ALIGNMENT.md` (template `0.11.0` + FOSS Cursor surface from `v0.21.0`)
- **Not adopted:** template Pages/release-please automerge, plugin packaging, branding-kit README rewrite
