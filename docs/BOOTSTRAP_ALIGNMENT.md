# Bootstrap Alignment — Gap Analysis

> **Date:** 2026-08-20  
> **Local template version:** `0.11.0` (`.template-version`)  
> **Upstream latest:** `v0.21.0` (`edwardlthompson/agent-project-bootstrap`)  
> **Mode:** Targeted sync — not a re-scaffold.

## Repo identity

| Fact | Value |
|------|-------|
| Product | Screen Wakelock Detector — FOSS Android app |
| Production path | `app/` (not `examples/android/`) |
| Stack | `.cursor/stack-selection.json` → **android** |
| License | Apache-2.0 (intentional vs template MIT) |
| Forges | GitHub CI/releases + GitLab MRs |

---

## ✅ Already current (no action)

| Artifact | Notes |
|----------|-------|
| `AGENTS.md`, `docs/START_HERE.md`, `CURSOR_MODES.md`, `FOR_AGENTS.md` | Present; project-customized |
| `BUILD_PLAN.md` labels AGENT/HUMAN/ADB/AUTO | Present (status markers still GitHub checkboxes) |
| `AGENT_MEMORY.md`, `DECISION_LOG.md`, `COMPLETED_TASKS.md` | Present + product-filled |
| Security/privacy: `SECURITY.md`, `SECURITY_TRIAGE.md`, `THREAT_MODEL.md`, `PRIVACY.md` | Present |
| Batch commands (20 atomic + 5 super, minus newer extras) | Present; SWD-custom `gates`/`prerelease`/`regress` |
| `.cursor/rules/*.mdc` (14 files incl. `project.mdc`) | Present except newer upstream rules |
| Hygiene/gates scripts, smoke suite, F-Droid scripts | Present and project-specific |
| Android CI: `android-ci.yml`, `release.yml`, `fdroid-publish.yml` | Present — do not replace with template `ci.yml`/`pages.yml` |
| `.editorconfig`, `.cursorignore`, `.env.example`, `.template-version` | Present |
| `modules/android/MODULE.md` | Active module |

## ⚠️ Outdated / partial

| Artifact | Gap | After this pass |
|----------|-----|-----------------|
| `.template-version` `0.11.0` vs upstream `0.21.0` | Child lag; last_checked null | Still deferred (intentional) |
| `AGENTS.md` / `CURSOR_MODES.md` / `FOR_AGENTS.md` | Missing FOSS hooks/skills/local-compute / 0.19+ coach-tour | ✅ Merged SWD notes + FOSS surface |
| `.cursor/rules/batch-commands.mdc` + registries | Still 25 commands; upstream has cleanup/coach/tour/ideas/codex-review | ✅ 24+5; `/codex-review` still skipped |
| `docs/START_HERE.md` / `BOOTSTRAP_TEMPLATE_MAP.md` | Still cite v0.11.0 | ✅ Note FOSS surface from v0.21.0 |
| `BUILD_PLAN.md` status | Template: 🔲/✅/❌ only — this repo still uses `- [ ]` | ✅ Converted |
| `core-directives.mdc` file-size line | Upstream: 300L static / 150L logic | ✅ Updated |

## ❌ Missing (upstream FOSS surface)

| Item | Since | Adopt? |
|------|-------|--------|
| `.cursor/hooks.json` + `.cursor/hooks/*` | 0.12–0.14 | **Yes** |
| `.cursor/skills/*`, `.cursor/agents/*` | 0.12 | **Yes** |
| `.cursor/worktrees.json`, setup-worktree scripts | 0.12+ | **Yes** |
| `.cursor/permissions.json`, `sandbox.json.example`, `mcp.foss.example`, `cursor-features.json` | 0.12–0.15 | **Yes** |
| `.cursor/rules/local-compute.mdc` | 0.15 | **Yes** |
| `/cleanup` command | 0.12 | **Yes** |
| `docs/FILE_SIZE_GUIDE.md` | 0.12 | **Yes** |
| FOSS Cursor docs (`CURSOR_INTEGRATIONS.md`, `CURSOR_CLI.md`, feature radar/registry, help/CURSOR_FEATURES.md) | 0.12–0.15 | **Yes** |
| `HUMAN_BACKLOG.md` (+ example) | 0.12+ | **Yes** |
| `KNOWLEDGE_BASE.md` | seed | **Yes** (empty/seed — do not invent KB entries) |
| `docs/UPGRADING_FROM_TEMPLATE.md` | — | **Yes** |
| `agent-run.py` + cursor check scripts + `scripts/lib/*` needed by them | 0.12–0.14 | **Yes** |
| `/coach`, `/tour`, `/ideas` + help docs | 0.19 | **Yes** (thin; point at SWD START_HERE) |
| `/codex-review` + `docs/CODEX_REVIEW.md` | 0.16 | Optional — adopt command stub if registry updated |
| Portable adapters: `CLAUDE.md`, `GEMINI.md`, `.clinerules` | 0.19 | **Yes** if they only route to AGENTS.md |
| `TEMPLATE_INDEX.json` | — | **Yes** (android-child index; validator already exists) |

## ❌ Missing — skip (not this stack / high risk)

| Item | Why skip |
|------|----------|
| Commercial Cursor (`commercial-compliance.mdc`, `*.commercial.example`) | FOSS / F-Droid |
| `pages.yml`, web/python/go/rust/node examples | Android-only pruned |
| Template `release-please-automerge.yml` / `action.yml` / `.cursor-plugin` | Custom `release.yml` + F-Droid |
| Branding kit / pitch README rewrite (0.17–0.20) | Would overwrite product README |
| `justfile`, `bootstrap.config.json` maintainer packaging | Template-maintainer |

## Preserve (do not overwrite)

- All of `app/**` (including uncommitted display-refresh work)
- Apache-2.0, no `INTERNET`, libsu allowlist, `docs/ROOT.md`
- `.cursor/rules/project.mdc`
- Customized `.cursor/commands/{gates,prerelease,regress,ci,init}.md`
- Smoke scripts, F-Droid metadata, Android workflows
- Product memory/ADR/GATES/COMPLETED history
- Dual archive (`COMPLETED_TASKS.md` + `docs/COMPLETED.md`)

---

## Sequential close plan

1. Write this report
2. Import FOSS Cursor hooks/skills/agents/worktrees + `local-compute.mdc`
3. Add `/cleanup` (+ coach/tour/ideas if registry updated together)
4. Seed `HUMAN_BACKLOG.md`, `KNOWLEDGE_BASE.md`; add FOSS Cursor docs + `FILE_SIZE_GUIDE.md`
5. Refresh agent docs with SWD appendices (merge, not blind overwrite)
6. Add minimal `TEMPLATE_INDEX.json`; keep `.template-version` at `0.11.0` until gates pass (note FOSS surface from `v0.21.0`)
7. Run `validate-bootstrap --quick`, encoding, batch-command check

### Critique

- **Null:** Hooks fail-open if Cursor version lacks events.
- **Races:** Do not replace Android CI with template `ci.yml`.
- **Exceptions:** Adding `/cleanup` without updating `check-batch-commands.sh` fails the orphan gate.
- **UTF-8:** Windows copies via Python UTF-8; run encoding check after import.

## Status (2026-08-20 close pass)

| Item | Status |
|------|--------|
| Gap report | ✅ |
| FOSS Cursor hooks/skills/agents/worktrees/`local-compute` | ✅ |
| `/cleanup` `/coach` `/tour` `/ideas` + registry | ✅ |
| Seed `HUMAN_BACKLOG.md` / `KNOWLEDGE_BASE.md` / `TEMPLATE_INDEX.json` | ✅ |
| Agent docs merge + SWD notes | ✅ |
| Full `.template-version` bump to `0.21.0` | 🔲 Deferred |
| Commercial / Pages / branding-kit README | ✅ Skipped |
