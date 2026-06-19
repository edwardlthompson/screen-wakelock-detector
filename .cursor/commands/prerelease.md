# Pre-release gate — Screen Wakelock Detector

```bash
bash scripts/pre-release-gate.sh
./gradlew lint test assembleDebug
bash scripts/smoke/m14_smoke.sh
```

When `[ADB]` device connected, also run:

```bash
bash scripts/smoke/m14_regression.sh
bash scripts/benchmark/memory_baseline.sh
```

Confirm Gate **G_RELEASE** in [`docs/GATES.md`](docs/GATES.md). See [`docs/PROJECT_ALIGNMENT.md`](docs/PROJECT_ALIGNMENT.md) pre-release protocol.

Do not tag or `/push` until gates pass. Signed release: `bash scripts/release/build-signed-apk.sh`.

Begin now.
