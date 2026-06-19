# Completed Tasks

Archive of finished BUILD_PLAN sprints. Product milestone history: [`docs/COMPLETED.md`](docs/COMPLETED.md).

---

## Sprint AR — Audit review (2026-06-19)

**Gate:** Local audit — bootstrap/feature-gate/gradle PASS; `watch-agent-gates.sh --step ar-audit` PASS. See ephemeral [`CODE_REVIEW.md`](CODE_REVIEW.md) (gitignored).

- [x] [AGENT] F-001 — `check-github-ci.sh` auto-detect **Android CI** when CodeQL/Trivy absent
- [x] [AGENT] F-002 — Add `scripts/count-critical-high-dependabot.sh` (0 Critical/High open)
- [x] [AGENT] F-003 — `docs/FOR_AGENTS.md` SWD CI workflow note
- [x] [AGENT] `CODE_REVIEW.md` written; `.gitignore` entry added
- [ ] [HUMAN] F-004 — Private vulnerability reporting (deferred to BUILD_PLAN backlog)
- [ ] [HUMAN] F-005/F-006 — GitLab + F-Droid (existing backlog)

## Sprint TM — Template migration (2026-06-19)

**Gate:** `Smoke TM: PASS 2026-06-19T10:03:34Z b5214fc6 1.2.12` — see [`docs/GATES.md`](docs/GATES.md)

- [x] [AGENT] agent-project-bootstrap v0.11.0 alignment — 25 slash commands, 14 Cursor rules, bootstrap gate scripts
- [x] [AGENT] Root `BUILD_PLAN.md`, `AGENT_MEMORY.md`, `CHANGELOG.md`, `DECISION_LOG.md`, `docs/BOOTSTRAP_TEMPLATE_MAP.md`
- [x] [AGENT] `validate-bootstrap.sh --quick` PASS · `feature-gate.sh --stack android` PASS · `watch-agent-gates.sh --step tm9` PASS
- [x] [AGENT] `./gradlew lint test assembleDebug` PASS (JAVA_HOME = Android Studio JBR)
- [x] [ADB] Wireless CPH2583 `b5214fc6` @ `192.168.1.2:44487` — `m14_regression`, `m13_adb_verify`, memory baseline seeded (`CPH2583.json`)
- [x] [AGENT] Fix `memory_baseline.sh` — sanitize `:` in wireless ADB serial for Windows artifact paths
