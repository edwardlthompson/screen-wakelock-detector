# CI status — Screen Wakelock Detector

Check GitHub Actions (primary CI for this repo):

```bash
gh run list --workflow android-ci.yml --limit 3
gh run watch --exit-status
```

Optional — wait for latest run on current branch:

```bash
bash scripts/check-github-ci.sh HEAD --wait 300
```

GitLab pipeline (when project is live): see [`docs/GITLAB.md`](docs/GITLAB.md).

Report pass/fail. Do not `/push` if Android CI is red.

Begin now.
