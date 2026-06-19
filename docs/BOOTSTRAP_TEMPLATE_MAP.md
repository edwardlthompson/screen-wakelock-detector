# Bootstrap Template Map — Screen Wakelock Detector

Maps [agent-project-bootstrap](https://github.com/edwardlthompson/agent-project-bootstrap) v**0.11.0** to this **android** child repo.

**Production path locked:** `app/` — do not relocate to `examples/android/`.

## Root documentation

| Template path | SWD path | Notes |
|---------------|----------|-------|
| `BUILD_PLAN.md` | `BUILD_PLAN.md` | Active sprint board (root) |
| `AGENTS.md` | `AGENTS.md` | Agent router |
| `AGENT_MEMORY.md` | `AGENT_MEMORY.md` | Living memory (root) |
| `COMPLETED_TASKS.md` | `COMPLETED_TASKS.md` + `docs/COMPLETED.md` | Dual archive |
| `DECISION_LOG.md` | `DECISION_LOG.md` | Major trade-offs |
| `docs/DESIGN_GUIDE.md` | Pointer → `docs/DESIGN_SYSTEM.md` | M3 project tokens |
| `examples/android/` | **`app/`** | Production Gradle module |
| `docs/GATES.md` | `docs/GATES.md` | Milestone + smoke log |

## Golden Path

| Template concept | SWD production | Template stub |
|------------------|----------------|---------------|
| Gradle / Kotlin | `app/build.gradle.kts`, `gradlew` | _(not used)_ |
| Compose UI | `app/src/main/java/.../ui/` | _(not used)_ |
| Unit tests | `app/src/test/` | _(not used)_ |
| F-Droid metadata | `fdroid/metadata/`, `fastlane/` | Template `examples/android/metadata/` |
| Smoke E2E | `scripts/smoke/m{N}_smoke.sh` | Template generic smoke |

## Gates

| Template script | SWD equivalent | Notes |
|-----------------|----------------|-------|
| `validate-bootstrap.sh --quick` | `scripts/validate-bootstrap.sh` | Android-only required files |
| `watch-agent-gates.sh` | `scripts/watch-agent-gates.sh` | → `feature-gate.sh --stack android` |
| `feature-gate.sh` | `scripts/feature-gate.sh` | `./gradlew lint test assembleDebug` at root |
| `pre-release-gate.sh` | `scripts/pre-release-gate.sh` | G_RELEASE + smoke scripts |
| Milestone smoke | `scripts/smoke/m14_smoke.sh` | Product regression |

## Cursor rules & commands

| Template | SWD |
|----------|-----|
| `.cursor/rules/batch-commands.mdc` | ✅ 25 slash commands |
| `.cursor/rules/project.mdc` | SWD-only: smoke-before-archive, libsu, GitLab protocol |
| `.cursor/commands/*.md` | ✅ Customized `gates`, `prerelease`, `regress`, `ci`, `init` |

## Intentionally omitted

- `examples/web/`, `examples/python/`, `examples/node/`, `examples/rust/`
- GitHub Pages / PWA workflows
- `release-please` for template version (`.template-version` only)
- Relocating `app/` into `examples/`

## Intentional divergences

| Template | SWD |
|----------|-----|
| MIT | Apache-2.0 |
| GitHub-primary | GitLab MRs + GitHub CI/releases |
| Milestone sprints | M0–M15 shipped; TM = bootstrap alignment |
