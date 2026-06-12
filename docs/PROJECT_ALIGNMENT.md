# Project template alignment

Maps the generic **Project Initialization Prompt** to Screen Wakelock Detector artifacts. This repo was not greenfield-reinitialized; gaps were closed incrementally (M14).

**License:** Apache-2.0 (unchanged) · **Distribution:** F-Droid only · **Primary forge:** GitLab MRs; GitHub for Actions, releases, Dependabot

---

## Template → project mapping

| Template requirement | This project |
|---------------------|--------------|
| Agent memory | [`AGENT_MEMORY.md`](AGENT_MEMORY.md) |
| Build plan + parallel tasks | [`BUILD_PLAN.md`](BUILD_PLAN.md) + `<!-- PARALLEL -->` blocks |
| Completed tasks archive | [`COMPLETED.md`](COMPLETED.md) via [`scripts/archive-completed-tasks.py`](../scripts/archive-completed-tasks.py) (template: `COMPLETED_TASKS.md`) |
| Changelog | [`CHANGELOG.md`](CHANGELOG.md) (Keep a Changelog) |
| Semantic versioning | `app/build.gradle.kts` + Git tags |
| Task labels AGENT/ADB/HUMAN | [`.cursor/AGENTS.md`](../.cursor/AGENTS.md), [`BUILD_PLAN.md`](BUILD_PLAN.md) |
| Gates | [`GATES.md`](GATES.md) + Gate GSM smoke log |
| Agent rules | [`.cursor/rules/project.mdc`](../.cursor/rules/project.mdc) |
| Agent orchestration | [`.cursor/AGENTS.md`](../.cursor/AGENTS.md); root [`AGENTS.md`](../AGENTS.md) entrypoint |
| CI lint/test/build | [`.gitlab-ci.yml`](../.gitlab-ci.yml), [`.github/workflows/android-ci.yml`](../.github/workflows/android-ci.yml) |
| Dependabot | [`.github/dependabot.yml`](../.github/dependabot.yml) |
| Security policy | [`SECURITY.md`](../SECURITY.md) |
| Unit regression | `app/src/test/**` |
| Device E2E | [`scripts/smoke/`](../scripts/smoke/) |
| FOSS policy | [`FOSS.md`](FOSS.md) |
| Architecture doc | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Contributing | [`CONTRIBUTING.md`](../CONTRIBUTING.md) |
| Code of conduct | [`CODE_OF_CONDUCT.md`](../CODE_OF_CONDUCT.md) |

---

## Intentional divergences

| Template default | Our choice |
|------------------|------------|
| MIT license | Apache-2.0 |
| GitHub-primary | GitLab MRs primary; GitHub for CI/releases |
| Sprint cadence | Milestone-driven (M0–M14) |
| Web perf (k6, Lighthouse) | ADB `dumpsys meminfo` benchmark script |
| Cloud APM | None — no `INTERNET` permission |
| Play Store | F-Droid only |

---

## Agent startup sequence

1. Read [`.cursor/AGENTS.md`](../.cursor/AGENTS.md), [`BUILD_PLAN.md`](BUILD_PLAN.md), [`GATES.md`](GATES.md)
2. Use Plan Mode for non-trivial work; investigate codebase first
3. Implement milestone tasks; parallelize `<!-- PARALLEL -->` blocks
4. Validate: `./gradlew lint test assembleDebug` + applicable smoke/ADB scripts
5. Update `CHANGELOG`, `AGENT_MEMORY`, `GATES`; archive only after smoke PASS
6. Pre-release: **Gate G_RELEASE** (includes memory baseline when device available)
7. Tag, release, record in `COMPLETED.md`

---

## Pre-release (Gate G_RELEASE)

Runs before every version tag. See [`GATES.md`](GATES.md) § Gate G_RELEASE.

Protocol (maps template “Debugging Agent”):

1. Full `./gradlew lint test assembleDebug`
2. Applicable `m{N}_smoke.sh` + ADB verify scripts — record serial in GATES
3. `scripts/benchmark/memory_baseline.sh` when `[ADB]` device connected — compares against `scripts/benchmark/baselines/devices/{MODEL}.json`; first run on new hardware seeds that file and PASSes
4. FOSS audit + no `INTERNET` in manifest (CI also checks)
5. Finalize `CHANGELOG [Unreleased]`; update `AGENT_MEMORY`
6. Signed APK verify — automated via `scripts/release/build-signed-apk.sh` / `publish-signed-release.sh` (calls `verify-signed-apk.sh`)
7. Optional debug: LeakCanary manual session before major releases (no release dependency)

Only after all applicable items pass: version bump, archive, commit, push, tag, publish.

---

## Sprints → milestones

Template “Sprints” map to **Milestones** in `BUILD_PLAN.md` (release/feature-driven, not timeboxed).
