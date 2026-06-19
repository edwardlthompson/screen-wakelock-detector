# Regression smoke — Screen Wakelock Detector

```bash
bash scripts/smoke/m14_smoke.sh
```

When `adb devices` shows a device (`[ADB]`):

```bash
bash scripts/smoke/m14_regression.sh
bash scripts/smoke/m13_adb_verify.sh
bash scripts/benchmark/memory_baseline.sh
```

Record result in [`docs/GATES.md`](docs/GATES.md). If no device, document smoke-blocked in [`AGENT_MEMORY.md`](AGENT_MEMORY.md) and continue other gates.

Begin now.
