# Repository hygiene

Track **source, tests, docs, CI, and `*.example` configs** only. Never commit build output, caches, secrets, or local live configs.

## Never track

| Path | Reason |
|------|--------|
| `node_modules/`, `__pycache__/`, `coverage/` | Tool caches |
| `dist/`, `build/`, `.gradle/` | Build output |
| `.env` | Secrets — use [`.env.example`](../.env.example) |
| `.app-update.json`, `donations.json` | Live copies — use `*.example` |
| `artifacts/` | Smoke screenshots, UI dumps, local reports |
| `.cursor/agent-progress.json` | Ephemeral agent state |
| `CODE_REVIEW.md` | Ephemeral audit output |
| `*.jks`, `keystore.properties` | Release signing |

See [`.gitignore`](../.gitignore) for the full list.

## Before every push

```bash
bash scripts/check-repo-hygiene.sh
bash scripts/check-tracked-artifacts.sh   # also run via hygiene orchestrator
```

## Reclaim disk

```bash
bash scripts/purge-ephemeral.sh          # dry-run
bash scripts/purge-ephemeral.sh --apply  # git clean -fdX (ignored untracked only)
```

## Agent commit discipline

1. Review `git status` before staging.
2. Stage **explicit paths** — avoid blind `git add -A` when live configs exist locally.
3. Run hygiene checks before push.

## Policies

- **No Git LFS** or **submodules** without `[HUMAN]` approval.
- **No force-push** to `main`.
- Cursor rules: [`.cursor/rules/repo-hygiene.mdc`](../.cursor/rules/repo-hygiene.mdc).

## Automation map

| Script | Purpose |
|--------|---------|
| `scripts/check-repo-hygiene.sh` | Orchestrates artifact + large-file + `.gitignore` checks |
| `scripts/check-tracked-artifacts.sh` | Fails on tracked APKs, `.env`, caches |
| `scripts/check-large-tracked-files.sh` | Size limits on tracked blobs |
| `scripts/purge-ephemeral.sh` | Safe cleanup of ignored untracked files |
