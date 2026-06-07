# Root stack (in-app)

Self-contained root attribution for **Screen Wakelock Detector** — no Shizuku, Magisk modules, Termux, or companion apps.

---

## What the user needs

| Requirement | Notes |
|-------------|-------|
| Device already rooted | Magisk, KernelSU, APatch, or equivalent — we do not perform rooting |
| One-time `su` grant | Standard root manager prompt |
| Nothing else | No modules, no Shizuku, no PC ADB for normal use |

---

## What ships in the APK

| Component | Purpose |
|-----------|---------|
| **[libsu](https://github.com/topjohnwu/libsu)** | Root shell IPC, session caching, background-safe execution |
| **`RootShellService`** | App-owned wrapper: preheat on opt-in, timeout, deny handling |
| **`RootCommandRunner`** | Executes allowlisted commands with output size caps |
| **`RootCommandAllowlist`** | Fixed enum — **never** interpolate user input |
| **`DumpsysPowerParser`** | Parses `dumpsys power` (API 29–35+ variants) |
| **`DumpsysBatteryStatsParser`** | Fallback from `dumpsys batterystats --checkin` |
| **`WakeupSourcesParser`** | `/sys/kernel/debug/wakeup_sources` when readable |
| **`RootAttributor`** | Merges root snapshot into `WakeEvent` at screen-on |

**Explicitly excluded:** Shizuku API, Magisk module ZIPs, Termux, external busybox requirement.

---

## Module layout

```
app/src/main/java/.../root/
  RootAvailability.kt
  RootShellService.kt
  RootCommandRunner.kt
  RootCommandAllowlist.kt
  parser/
    DumpsysPowerParser.kt
    DumpsysBatteryStatsParser.kt
    WakeupSourcesParser.kt
  RootAttributor.kt
app/src/test/resources/root/
  dumpsys_power_api29.txt
  dumpsys_power_api34.txt
  ...
```

---

## Allowlisted commands

Executed only via `RootCommandAllowlist` enum:

```text
dumpsys power
dumpsys batterystats --checkin
cat /sys/kernel/debug/wakeup_sources
```

**Security:** Unit tests must assert rejection of arbitrary strings. Timeouts and max output bytes enforced in `RootCommandRunner`.

---

## Settings → Root (in-app)

- **Root access** switch: ON → libsu `su` request; OFF → drop session (revoke in Magisk is user-managed)
- **Root diagnostics** row: last command, parse success, Android version — for `[ADB]`/`[HUMAN]` without PC
- **No “Install module”** or Shizuku CTAs anywhere

Non-root users see root-enhanced fields **disabled/grayed** with supporting text: “Requires root — all tooling is built into this app.”

---

## Attribution merge

On screen-on when root enabled:

1. Run allowlisted snapshot commands
2. Parse wakelock name, tag, uid, pid where available
3. Feed signals into correlator alongside notification/usage data
4. Set `rootEnhanced` flag on `WakeEvent`; show “Root enhanced” badge in UI

Root denied / timeout / parse failure → non-fatal; log diagnostics; continue non-root path.

---

## Limitations

- Cannot root the phone from the app
- `dumpsys` format varies by OEM and Android version → versioned parsers + fallback chain
- Some kernels block `wakeup_sources` → degrade to `dumpsys power` only
- Root does not bypass notification channel UX for mute/settings
- Root is optional; core logging works without it

---

## Testing

| Test | Owner |
|------|-------|
| Parser fixtures API 29, 31, 34 | `[AGENT]` unit tests |
| Allowlist rejection | `[AGENT]` unit tests |
| Rooted device wakelock tag | `[ADB]` m3_smoke.sh |
| Non-root grayed UI | `[ADB]` m3_smoke.sh |
| su deny / timeout | `[ADB]` manual |

Document device results in [`AGENT_MEMORY.md`](AGENT_MEMORY.md).
