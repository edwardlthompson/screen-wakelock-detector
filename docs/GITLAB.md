# GitLab setup and CI

Primary forge and CI/CD for **Screen Wakelock Detector**.

---

## Repository

| Field | Value |
|-------|-------|
| **Suggested path** | `screen-wakelock-detector` |
| **Visibility** | Public |
| **Default branch** | `main` (protected) |
| **Tag pattern** | `v*` → release + F-Droid pipeline |

### About string (≤120 characters)

Use this in GitLab project **Description** / About field:

```text
Find what wakes your screen. See the app and channel, mute it in one tap—all local, no cloud.
```

Character count: 97 (within 120 limit).

---

## Prerequisites

- GitLab account
- Git, JDK 17 (Temurin), Android SDK
- Optional: [`glab`](https://gitlab.com/gitlab-org/cli) CLI for MR automation

---

## Setup from scratch

| Step | Owner | Action |
|------|-------|--------|
| 1 | HUMAN | Create public GitLab project `screen-wakelock-detector` |
| 2 | HUMAN | Connect GitLab MCP in Cursor when available |
| 3 | AGENT | Push scaffold + `.gitlab-ci.yml` + `.fdroid.yml` |
| 4 | HUMAN | Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata); set CI variables |
| 5 | HUMAN | Protect `main`; require MR + green pipeline |
| 6 | AGENT | Create labels via API or glab |

---

## GitLab labels

| Label | Color suggestion | Use |
|-------|------------------|-----|
| `AGENT` | `#428BCA` | Agent-implementable tasks |
| `ADB` | `#FC6D26` | Needs physical device |
| `HUMAN` | `#754778` | Needs human review/action |
| `fdroid` | `#7BC043` | F-Droid / fdroiddata work |
| `milestone-M0` … `M7` | `#C91E24` | Milestone tracking |
| `gate-blocked` | `#D9534F` | Gate failure |
| `blocked` | `#ADB5BD` | External blocker |

Create via UI or:

```bash
glab label create AGENT --color "#428BCA" --description "Agent implements autonomously"
glab label create ADB --color "#FC6D26" --description "Needs USB device testing"
glab label create HUMAN --color "#754778" --description "Needs human review"
glab label create fdroid --color "#7BC043" --description "F-Droid pipeline"
```

---

## CI/CD variables

Settings → CI/CD → Variables:

| Variable | Masked | Owner | Purpose |
|----------|--------|-------|---------|
| `FDROIDDATA_FORK_URL` | No | HUMAN | e.g. `git@gitlab.com:you/fdroiddata.git` |
| `GITLAB_TOKEN` | Yes | HUMAN | glab MR creation |
| `CI_PUSH_TOKEN` | Yes | HUMAN | Bot commit for archive-build-plan job |
| `RELEASE_STORE_FILE` | Yes | HUMAN | Base64 keystore or file variable |
| `RELEASE_STORE_PASSWORD` | Yes | HUMAN | Keystore password |
| `RELEASE_KEY_ALIAS` | Yes | HUMAN | Key alias |
| `RELEASE_KEY_PASSWORD` | Yes | HUMAN | Key password |

Never commit secrets to the repository.

---

## Pipeline stages

See [`.gitlab-ci.yml`](../.gitlab-ci.yml):

| Stage | Jobs | Trigger |
|-------|------|---------|
| **validate** | license-check, foss-audit, no-internet-manifest | MR, main, tags |
| **test** | lint, detekt, unit-test | MR, main, tags |
| **build** | assemble-debug | MR, main |
| **release** | assemble-release | tags `v*` |
| **fdroid** | fdroid-build, reproducible-verify | tags; fdroid-build manual on main |
| **publish** | gitlab-release, fdroiddata-mr | tags `v*` |

Pipeline URL template:

```text
https://gitlab.com/<namespace>/screen-wakelock-detector/-/pipelines/<pipeline_id>
```

Record project URL and ID in [`AGENT_MEMORY.md`](AGENT_MEMORY.md).

---

## Branch model

- `main` — release-ready; protected
- `feature/*` — MR required; CI must pass
- Tags `v1.0.0`, `v1.1.0`, … — trigger release artifacts + F-Droid jobs

---

## GitLab MCP integration

When connected in Cursor, record in AGENT_MEMORY:

- Project URL and numeric project ID
- glab auth status (`glab auth status`)
- fdroiddata fork URL
- Default MR assignee for fdroiddata

**Agent capabilities:**

- Trigger pipeline on branch/tag
- Read failed job logs (`fdroid-build`, `reproducible-verify`)
- Open fdroiddata MR via CI or glab
- Comment on packager questions

---

## Archive automation

Job `archive-build-plan` on push to `main` when `docs/BUILD_PLAN.md` changes:

1. Runs `scripts/archive-completed-tasks.py`
2. Commits `COMPLETED.md` + updated BUILD_PLAN if diff
3. Uses `CI_PUSH_TOKEN` for bot commit

Precondition: smoke PASS in [`GATES.md`](GATES.md) for target milestone.

---

## glab examples

```bash
# Auth
glab auth login --hostname gitlab.com

# Create MR
glab mr create --title "feat(m2): attribution" --description "Gate G2"

# View pipeline
glab ci view

# Retry job
glab ci retry <job_id>
```

---

## Milestone protocol

After each milestone:

1. Run gate checklist + smoke script
2. Update CHANGELOG, GATES.md, AGENT_MEMORY
3. Run archive script (smoke required)
4. Commit with `feat(mN): ... — gate GX passed`
5. Push to `main`; confirm pipeline green

See [`.cursor/AGENTS.md`](../.cursor/AGENTS.md).

---

## Note on GitHub

GitHub Actions mentioned in early planning are **deprecated** in favor of GitLab CI. Optional GitHub mirror is out of scope unless requested.
