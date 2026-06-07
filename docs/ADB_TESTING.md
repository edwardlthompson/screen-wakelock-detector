# ADB testing and smoke scripts

Device validation for **Screen Wakelock Detector** before archiving milestone tasks.

Smoke scripts live in [`scripts/smoke/`](../scripts/smoke/). **Archive is blocked** until the matching milestone smoke PASS is recorded in [`GATES.md`](GATES.md).

---

## Prerequisites

| Requirement | Check |
|-------------|-------|
| Android SDK platform-tools | `adb version` |
| USB debugging enabled | Developer options on device |
| Device authorized | `adb devices` shows `device` (not `unauthorized`) |
| Debug APK built | `./gradlew assembleDebug` |
| Bash shell | Git Bash / WSL / Linux / macOS for `.sh` scripts |

**Package:** `com.screenwakelock.detector`

---

## Quick start

```bash
# 1. Build and install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Run milestone smoke (example M0)
bash scripts/smoke/m0_smoke.sh

# 3. On exit 0, record in docs/GATES.md:
#    Smoke M0: PASS 2026-06-06T12:00:00Z <serial> 0.1.0-debug

# 4. Archive (only after smoke PASS)
python scripts/archive-completed-tasks.py --milestone M0 --dry-run
python scripts/archive-completed-tasks.py --milestone M0
```

---

## Smoke execution flow

```text
1. adb install -r app/build/outputs/apk/debug/app-debug.apk
2. scripts/smoke/m{N}_smoke.sh   → exit 0 = PASS, non-zero = FAIL
3. On PASS: append to docs/GATES.md smoke log
4. Mark BUILD_PLAN [x] → run archive-completed-tasks.py
5. On FAIL: log output to AGENT_MEMORY; fix; re-run — do not archive
```

If no device at milestone end: mark `smoke-blocked` in AGENT_MEMORY — **do not archive or push** until smoke passes. M0 exception: scaffold may push if only smoke blocked, but M0 tasks stay in BUILD_PLAN until smoke runs.

---

## Milestone scripts

| Milestone | Script | Pass criteria |
|-----------|--------|---------------|
| **M0** | `m0_smoke.sh` | APK installs; app launches; M3 theme visible; no crash 30s |
| **M1** | `m1_smoke.sh` | Lock/unlock → new history row within 5s; FGS notification present |
| **M2** | `m2_smoke.sh` | Test notification → wake → attributed app+channel on detail |
| **M3** | `m3_smoke.sh` | Non-root: root rows grayed; rooted: wakelock tag on entry |
| **M4** | `m4_smoke.sh` | Last-wake card → bottom sheet → channel settings intent resolves |
| **M5** | `m5_smoke.sh` | Insights counts match history; threshold alert on synthetic burst |
| **M6** | `m6_smoke.sh` | Widget shows last wake; tap opens app; heatmap renders |
| **M7** | `m7_smoke.sh` | F-Droid automation scripts present; verify-reproducible.sh runnable |
| **M8** | `m8_smoke.sh` | Verify script + icon; Settings + Insights; WakeCountWidget; no crash |
| **M8 deep** | `m8_adb_deep_verify.sh` | Root timeline on Detail; Share diagnostic report; pattern Mute/Open; batch mute dialog (OnePlus CPH2583) |

---

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `ADB` | `adb` | adb binary path |
| `PACKAGE` | `com.screenwakelock.detector` | App package |
| `APK_PATH` | `app/build/outputs/apk/debug/app-debug.apk` | Install path |
| `SMOKE_TIMEOUT` | varies per script | Command timeout seconds |

---

## Manual fallback checklists

Use when automation cannot cover OEM-specific behavior.

### M2 attribution manual

1. Grant notification access in system settings
2. From second device or `adb shell cmd notification post`, post heads-up notification
3. Turn screen off, wait for notification to wake screen
4. Open app → verify package + channel on detail screen

### M4 mute manual

1. Open wake detail → Silence channel
2. Post another notification on same channel — verify suppressed or importance none
3. Test Undo snackbar within 10s

### M5 threshold alert manual

1. Enable alerts in Settings
2. Grant POST_NOTIFICATIONS
3. Trigger 5+ wakes from same channel within 1 hour (or lower threshold in debug)
4. Verify notification title includes app + channel

---

## Useful adb commands

```bash
# Launch app
adb shell am start -n com.screenwakelock.detector/.MainActivity

# Logcat filter
adb logcat -s ScreenWakelockDetector:*

# Dump notification listener status
adb shell cmd notification allowed_listeners

# Force screen on/off
adb shell input keyevent KEYCODE_POWER

# Post test notification (API 29+, may need helper app)
adb shell cmd notification post -S bigtext -t "Test" "Tag" "Body"
```

---

## CI note

Smoke scripts are **not** run in GitLab CI by default (no emulators in validate stage). `[ADB]` owner runs on physical device before archive.

Optional future job: connected Android emulator on tagged runners.

---

## Troubleshooting

| Symptom | Action |
|---------|--------|
| `device unauthorized` | Accept RSA fingerprint on phone |
| Install fails `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `adb uninstall com.screenwakelock.detector` |
| No history after unlock | Check battery optimization + FGS notification |
| Script exits 127 | Run with `bash scripts/smoke/...` not `sh` on Windows |

Log failures to [`AGENT_MEMORY.md`](AGENT_MEMORY.md) with device model and Android version.
