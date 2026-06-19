# Existing-repo bootstrap — Screen Wakelock Detector

This repo was **not** created via **Use this template**. Run maintenance init only:

1. Confirm [`docs/BOOTSTRAP_TEMPLATE_MAP.md`](docs/BOOTSTRAP_TEMPLATE_MAP.md) — production path is `app/`.
2. Verify `.template-version` and `.cursor/stack-selection.json` (`stack: android`).
3. Run (non-destructive):
   ```bash
   bash scripts/init-project.sh --stack android --non-interactive --no-prune \
     --project-name "Screen Wakelock Detector" \
     --purpose "FOSS Android app — screen wake attribution and quick fixes" \
     --release-repo edwardlthompson/screen-wakelock-detector
   ```
4. Run `bash scripts/validate-bootstrap.sh --quick` and `bash scripts/feature-gate.sh --stack android`.
5. Read [`docs/START_HERE.md`](docs/START_HERE.md) and [`BUILD_PLAN.md`](BUILD_PLAN.md) Sequential lane.

**Do not** prune `app/` or run greenfield scaffold steps.

Begin now.
