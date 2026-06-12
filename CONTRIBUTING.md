# Contributing

Thank you for helping improve Screen Wakelock Detector. This project is Apache-2.0, F-Droid-only, and ships with **no `INTERNET` permission**.

---

## For humans

1. **Fork** on [GitLab](https://gitlab.com/) (primary forge) or [GitHub](https://github.com/edwardlthompson/screen-wakelock-detector) (mirror / CI).
2. Create a branch: `feature/short-description` or `fix/issue-description`.
3. Implement your change following [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) (Material 3 only).
4. Run locally:

   ```bash
   ./gradlew lint test assembleDebug
   ```

5. Open a merge request (GitLab) or pull request (GitHub). CI must pass.
6. New dependencies: read [`docs/FOSS.md`](docs/FOSS.md) and document non-trivial decisions in [`docs/AGENT_MEMORY.md`](docs/AGENT_MEMORY.md).

Device smoke tests (when you have USB): see [`docs/ADB_TESTING.md`](docs/ADB_TESTING.md).

---

## For agents

1. Read [`AGENTS.md`](AGENTS.md) → [`.cursor/AGENTS.md`](.cursor/AGENTS.md) first.
2. Work from [`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) milestone tasks (`[AGENT]` / `[ADB]` / `[HUMAN]`).
3. Validate against [`docs/GATES.md`](docs/GATES.md) before archiving.
4. Pre-release: Gate **G_RELEASE** in GATES (see [`docs/PROJECT_ALIGNMENT.md`](docs/PROJECT_ALIGNMENT.md)).
5. Archive completed tasks only after smoke PASS: `python scripts/archive-completed-tasks.py --milestone M{N}`.

---

## Code style

- Kotlin, Jetpack Compose Material 3 (`androidx.compose.material3` — never Material 2)
- Match existing patterns in surrounding files; minimal scope per change
- Unit tests for non-trivial domain logic

---

## Security

Report vulnerabilities per [`SECURITY.md`](SECURITY.md). Do not open public issues for sensitive reports.

---

## Code of conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Be respectful and constructive.
