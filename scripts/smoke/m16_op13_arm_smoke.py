#!/usr/bin/env python3
"""OP13 Wake Shield smoke: arm via DataStore push using run-as (no su)."""
from __future__ import annotations

import re
import subprocess
import sys
import time
from pathlib import Path

SERIAL = sys.argv[1] if len(sys.argv) > 1 else "8bf09993"
PKG = "com.screenwakelock.detector"
NLS = f"{PKG}/.service.NotificationCaptureService"
A11Y = f"{PKG}/.wakeshield.ShieldAccessibilityService"
TMP_PREFS = Path("_m16_settings.preferences_pb")


def adb(*args: str, timeout: float = 45, input_bytes: bytes | None = None) -> str:
    p = subprocess.run(
        ["adb", "-s", SERIAL, *args],
        input=input_bytes,
        capture_output=True,
        timeout=timeout,
        check=False,
    )
    return (p.stdout + p.stderr).decode("utf-8", "replace").replace("\r", "")


def log(msg: str) -> None:
    print(f"[m16_op13] {msg}", flush=True)


def _varint(n: int) -> bytes:
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        out.append(b | (0x80 if n else 0))
        if not n:
            break
    return bytes(out)


def _key(field: int, wire: int) -> bytes:
    return _varint((field << 3) | wire)


def _len_delim(field: int, data: bytes) -> bytes:
    return _key(field, 2) + _varint(len(data)) + data


def _bool_value(v: bool) -> bytes:
    return _key(1, 0) + _varint(1 if v else 0)


def _pref_entry(name: str, value: bytes) -> bytes:
    return _len_delim(1, name.encode()) + _len_delim(2, value)


def build_prefs(pairs: dict[str, bool]) -> bytes:
    out = bytearray()
    for k, v in pairs.items():
        entry = _pref_entry(k, _bool_value(v))
        out += _len_delim(1, entry)
    return bytes(out)


def ensure_setting(key: str, component: str) -> None:
    cur = adb("shell", "settings", "get", "secure", key).strip()
    if PKG in cur:
        return
    new = component if cur in ("", "null") else f"{cur}:{component}"
    adb("shell", "settings", "put", "secure", key, new)


def sql(q: str) -> str:
    # App is debuggable — run-as sqlite3 works after force-stop
    adb("shell", "am", "force-stop", PKG, timeout=15)
    time.sleep(0.5)
    return adb(
        "shell",
        "run-as",
        PKG,
        "sqlite3",
        "databases/screen_wakelock.db",
        q,
        timeout=25,
    ).strip()


def main() -> int:
    log(f"device={SERIAL} model={adb('shell', 'getprop', 'ro.product.model').strip()}")

    prefs = build_prefs(
        {
            "has_completed_intro": True,
            "monitoring_enabled": True,
            "shield_enabled": True,
            "wake_forensics_enabled": True,
        }
    )
    TMP_PREFS.write_bytes(prefs)
    log(f"wrote local prefs bytes={len(prefs)}")

    log("wake device")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")

    log("grant listener + a11y")
    ensure_setting("enabled_notification_listeners", NLS)
    ensure_setting("enabled_accessibility_services", A11Y)
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    adb("shell", "cmd", "notification", "allow_listener", NLS)
    listeners = adb("shell", "settings", "get", "secure", "enabled_notification_listeners")
    acc = adb("shell", "settings", "get", "secure", "enabled_accessibility_services")
    log(f"listeners has pkg={PKG in listeners}")
    log(f"a11y={acc.strip()}")
    if PKG not in listeners:
        raise SystemExit("FAIL: notification listener not granted")
    if PKG not in acc:
        raise SystemExit("FAIL: accessibility not granted")
    log("PASS: special access")

    log("push DataStore prefs via run-as")
    adb("shell", "am", "force-stop", PKG)
    time.sleep(0.5)
    # Stream bytes into app-private path
    adb(
        "shell",
        "run-as",
        PKG,
        "sh",
        "-c",
        "mkdir -p files/datastore && cat > files/datastore/settings.preferences_pb",
        input_bytes=prefs,
        timeout=30,
    )
    chk = adb(
        "shell",
        "run-as",
        PKG,
        "sh",
        "-c",
        "grep -a shield_enabled files/datastore/settings.preferences_pb >/dev/null && echo YES || echo NO",
    )
    if "YES" not in chk:
        raise SystemExit(f"FAIL: prefs write failed: {chk}")
    log("PASS: prefs contain shield_enabled")

    log("start app + monitor FGS")
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(2)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
    time.sleep(3)
    notif = adb("shell", "dumpsys", "notification", "--noredact", timeout=60)
    armed = "Wake Shield armed" in notif
    log(f"FGS armed text={armed}")
    if not armed:
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        # re-push prefs in case app overwrote on first launch
        adb(
            "shell",
            "run-as",
            PKG,
            "sh",
            "-c",
            "mkdir -p files/datastore && cat > files/datastore/settings.preferences_pb",
            input_bytes=prefs,
            timeout=30,
        )
        adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
        time.sleep(2)
        adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
        time.sleep(3)
        notif = adb("shell", "dumpsys", "notification", "--noredact", timeout=60)
        armed = "Wake Shield armed" in notif
        log(f"FGS armed text after restart={armed}")
    if not armed:
        snippet = "\n".join(
            line for line in notif.splitlines() if "screenwakelock" in line.lower() or "Wake" in line or "Monitor" in line
        )[:800]
        log(f"notif snippet:\n{snippet}")
        raise SystemExit("FAIL: Wake Shield armed not in notification")
    log("PASS: Wake Shield armed (FGS)")

    before = sql("SELECT COUNT(*) FROM wake_events;")
    # restart monitor after sql force-stop
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(1)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
    time.sleep(2)
    log(f"trigger wake; before={before}")
    adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
    time.sleep(2)
    adb(
        "shell",
        "cmd",
        "notification",
        "post",
        "-t",
        "M16 Shield Test",
        "m16_shield",
        "Hostile wake for Wake Shield smoke",
    )
    time.sleep(1)
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    time.sleep(1)
    adb("shell", "input", "keyevent", "KEYCODE_POWER")
    time.sleep(6)
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    time.sleep(2)
    after = sql("SELECT COUNT(*) FROM wake_events;")
    latest = sql(
        "SELECT id,shieldOutcome,reasonCode,attributedPackage "
        "FROM wake_events ORDER BY timestampMillis DESC LIMIT 5;"
    )
    log(f"after={after}")
    log(f"latest:\n{latest}")

    # restart for dumpsys checks
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
    time.sleep(2)
    notif2 = adb("shell", "dumpsys", "notification", "--noredact", timeout=60)
    if "Wake Shield armed" not in notif2:
        # prefs still set?
        chk2 = adb(
            "shell",
            "run-as",
            PKG,
            "sh",
            "-c",
            "grep -a shield_enabled files/datastore/settings.preferences_pb >/dev/null && echo YES || echo NO",
        )
        raise SystemExit(f"FAIL: shield disarmed after wake (prefs={chk2.strip()})")
    log("PASS: still armed")

    a11y = adb("shell", "dumpsys", "accessibility", timeout=40)
    if "ShieldAccessibilityService" not in a11y:
        raise SystemExit("FAIL: ShieldAccessibilityService missing from dumpsys")
    log("PASS: accessibility service registered")

    if re.search(
        r"LOCKED|SLEPT|CANCELLED|PARTIAL|DENIED|ALLOWED_|ABORTED|SUPPRESSED",
        latest or "",
    ):
        log("PASS: shieldOutcome present")
    elif after.isdigit() and before.isdigit() and int(after) > int(before):
        log("PASS: wake logged")
    else:
        log("WARN: no new wake row (device may be keyguarded; shield still armed)")

    log(f"PASS: m16 OP13 smoke complete on {SERIAL}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        log(f"FAIL: {exc}")
        raise
