# Parallel agent scopes — Screen Wakelock Detector

Single-module Android app (`app/`). Use this map before dispatching parallel Task subagents. Run `scripts/check-parallel-scope.sh` when available; otherwise verify no overlapping paths manually.

## Conflict rule

Only the **lead agent** commits and pushes per milestone. Parallel agents work on separate branches/worktrees.

## Safe parallel lanes

| Lane | Paths | Notes |
|------|-------|-------|
| **Domain / attribution** | `app/.../domain/**` | Pure logic + unit tests; no UI |
| **Data / Room** | `app/.../data/**`, `app/schemas/**` | Schema migrations are **sequential only** |
| **UI screens** | `app/.../ui/screens/**` | One screen per agent when possible |
| **UI theme** | `app/.../ui/theme/**` | Sequential if multiple agents touch tokens |
| **Widgets / Glance** | `app/.../widget/**`, `app/.../glance/**` | |
| **Root parsers** | `app/.../root/**`, `app/src/test/resources/root/**` | Shared fixtures: sequential first |
| **Smoke / ADB** | `scripts/smoke/**` | Device scripts; no overlap on same `m{N}_smoke.sh` |
| **Docs (non-overlapping)** | `docs/*.md` (distinct files) | Do not edit `GATES.md` / `BUILD_PLAN.md` in parallel |
| **Release / CI** | `scripts/release/**`, `.github/workflows/**` | One agent per workflow file |

## Sequential only (never parallel)

| Area | Why |
|------|-----|
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Dependency graph |
| Room schema / migrations | Migration ordering |
| Shared navigation / `AppNavigation.kt` | Route conflicts |
| `BUILD_PLAN.md`, `AGENT_MEMORY.md`, `CHANGELOG.md` | Lead agent only |
| `app/.../di/**` Hilt modules | Binding graph |

## Screen Wakelock Detector defaults

- **Stack:** Android only — ignore `examples/` unless explicitly in scope.
- **Production path:** `app/src/main/java/com/screenwakelock/detector/`
- **Parallel blocks:** Follow `<!-- PARALLEL -->` in [`BUILD_PLAN.md`](../BUILD_PLAN.md).

## Dispatch checklist

1. Assign non-overlapping path globs from the table above.
2. Branch: `feature/agent-<task>` per subagent.
3. After merge: lead runs `bash scripts/watch-agent-gates.sh --once --autofix --step <label>`.
