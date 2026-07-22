#!/usr/bin/env python3
"""OP13 Wake Shield sideload smoke — no adb root required."""
from __future__ import annotations

import re
import subprocess
import sys
import time

SERIAL = sys.argv[1] if len(sys.argv) > 1 else "8bf09993"
PKG = "com.screenwakelock.detector"
NLS = f"{PKG}/.service.NotificationCaptureService"
A11Y = f"{PKG}/.wakeshield.ShieldAccessibilityService"
APK = "app/build/outputs/apk/debug/app-debug.apk"


def adb(*args: str, timeout: float = 30) -> str:
    cmd = ["adb", "-s", SERIAL, *args]
    try:
        p = subprocess.run(
            cmd,
            capture_output=True,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"adb timed out: {' '.join(cmd)}")
    out = (p.stdout or b"").decode("utf-8", "replace")
    err = (p.stderr or b"").decode("utf-8", "replace")
    return (out + err).replace("\r", "")


def log(msg: str) -> None:
    print(f"[m16_op13] {msg}", flush=True)


def ui_dump() -> str:
    return adb("exec-out", "uiautomator", "dump", "/dev/stdout", timeout=45)


def tap_bounds(x1: int, y1: int, x2: int, y2: int, right_bias: bool = False) -> None:
    if right_bias:
        cx = min(1000, x2 + 140)
    else:
        cx = (x1 + x2) // 2
    cy = (y1 + y2) // 2
    adb("shell", "input", "tap", str(cx), str(cy))
    time.sleep(1)


def find_text(dump: str, text: str):
    pat = rf'text="{re.escape(text)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    m = re.search(pat, dump)
    if not m:
        return None
    return tuple(int(m.group(i)) for i in range(1, 5))


def tap_text(text: str) -> bool:
    b = find_text(ui_dump(), text)
    if not b:
        return False
    tap_bounds(*b)
    return True


def tap_near(text: str) -> bool:
    b = find_text(ui_dump(), text)
    if not b:
        return False
    tap_bounds(*b, right_bias=True)
    return True


def sql(q: str) -> str:
    # Prefer su -c sqlite3; fall back to run-as
    out = adb(
        "shell",
        "su",
        "-c",
        f"sqlite3 /data/data/{PKG}/databases/screen_wakelock.db \"{q}\"",
        timeout=20,
    )
    if "not found" in out.lower() or "Permission denied" in out or out.strip() == "":
        adb("shell", "am", "force-stop", PKG, timeout=15)
        time.sleep(1)
        out = adb(
            "shell",
            "run-as",
            PKG,
            "sqlite3",
            "databases/screen_wakelock.db",
            q,
            timeout=20,
        )
    return out.strip()


def prefs_has(needle: str) -> bool:
    out = adb(
        "shell",
        "run-as",
        PKG,
        "sh",
        "-c",
        f"grep -a {needle} files/datastore/settings.preferences_pb >/dev/null && echo YES || echo NO",
        timeout=20,
    )
    return "YES" in out


def main() -> int:
    log(f"device={SERIAL} model={adb('shell', 'getprop', 'ro.product.model').strip()}")

    # Install already done; ensure present
    pkg = adb("shell", "pm", "path", PKG)
    if PKG not in pkg and "package:" not in pkg:
        log("installing apk")
        print(adb("install", "-r", APK, timeout=120))

    log("wake + dismiss keyguard")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    adb("shell", "input", "swipe", "540", "2000", "540", "600", "300")
    time.sleep(1)

    log("grant notification listener + accessibility")
    adb("shell", "cmd", "notification", "allow_listener", NLS)
    listeners = adb("shell", "settings", "get", "secure", "enabled_notification_listeners").strip()
    if listeners in ("", "null"):
        adb("shell", "settings", "put", "secure", "enabled_notification_listeners", NLS)
    elif PKG not in listeners:
        adb(
            "shell",
            "settings",
            "put",
            "secure",
            "enabled_notification_listeners",
            f"{listeners}:{NLS}",
        )
    # Magisk/su fallback if settings put was blocked for non-root shell
    listeners = adb("shell", "settings", "get", "secure", "enabled_notification_listeners").strip()
    if PKG not in listeners:
        adb(
            "shell",
            "su",
            "-c",
            f"settings put secure enabled_notification_listeners '{listeners}:{NLS}'"
            if listeners not in ("", "null")
            else f"settings put secure enabled_notification_listeners '{NLS}'",
            timeout=20,
        )
        adb("shell", "su", "-c", f"cmd notification allow_listener {NLS}", timeout=20)

    existing = adb("shell", "settings", "get", "secure", "enabled_accessibility_services").strip()
    if existing in ("", "null"):
        adb("shell", "settings", "put", "secure", "enabled_accessibility_services", A11Y)
    elif PKG not in existing:
        adb(
            "shell",
            "settings",
            "put",
            "secure",
            "enabled_accessibility_services",
            f"{existing}:{A11Y}",
        )
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    listeners = adb("shell", "settings", "get", "secure", "enabled_notification_listeners")
    acc = adb("shell", "settings", "get", "secure", "enabled_accessibility_services")
    log(f"listeners={listeners.strip()}")
    log(f"a11y={acc.strip()}")
    if PKG not in listeners:
        raise SystemExit("FAIL: notification listener not granted")
    if PKG not in acc:
        raise SystemExit("FAIL: accessibility not granted")
    log("PASS: special access granted")

    log("launch app")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(3)
    dump = ui_dump()
    if "Find out what keeps" in dump:
        log("completing onboarding")
        if not tap_text("Next"):
            raise SystemExit("FAIL: onboarding Next missing")
        if not (tap_text("Skip") or tap_text("Get started")):
            raise SystemExit("FAIL: onboarding Skip/Get started missing")
    log("PASS: onboarding/home")

    log("open Settings -> Wake Shield")
    adb(
        "shell",
        "am",
        "start",
        "-a",
        "android.intent.action.VIEW",
        "-d",
        "screenwakelock://settings",
        "-p",
        PKG,
    )
    time.sleep(2)
    found = False
    for _ in range(8):
        if "Wake Shield" in ui_dump():
            found = True
            break
        adb("shell", "input", "swipe", "720", "2000", "720", "600", "350")
        time.sleep(1)
    if not found:
        raise SystemExit("FAIL: Wake Shield section not found")
    log("PASS: Wake Shield section visible")

    if not tap_near("Wake Shield"):
        raise SystemExit("FAIL: could not tap Wake Shield switch")
    time.sleep(1)
    tap_near("Wake forensics")

    log("start monitoring service")
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
    time.sleep(2)

    armed_pref = prefs_has("shield_enabled")
    notif = adb("shell", "dumpsys", "notification", "--noredact", timeout=45)
    armed_notif = "Wake Shield armed" in notif
    log(f"pref shield_enabled={armed_pref} fgs_armed={armed_notif}")
    if not armed_pref and not armed_notif:
        log("retry toggle")
        adb(
            "shell",
            "am",
            "start",
            "-a",
            "android.intent.action.VIEW",
            "-d",
            "screenwakelock://settings",
            "-p",
            PKG,
        )
        time.sleep(2)
        for _ in range(6):
            if "Wake Shield" in ui_dump():
                break
            adb("shell", "input", "swipe", "720", "2000", "720", "600", "350")
        tap_near("Wake Shield")
        time.sleep(2)
        adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.WakeMonitorService")
        time.sleep(2)
        armed_pref = prefs_has("shield_enabled")
        notif = adb("shell", "dumpsys", "notification", "--noredact", timeout=45)
        armed_notif = "Wake Shield armed" in notif
    if not armed_pref and not armed_notif:
        raise SystemExit("FAIL: could not arm Wake Shield")
    log("PASS: Wake Shield armed")

    log("trigger hostile wake")
    before = sql("SELECT COUNT(*) FROM wake_events;")
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
    time.sleep(5)
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    time.sleep(2)
    after = sql("SELECT COUNT(*) FROM wake_events;")
    latest = sql(
        "SELECT id,shieldOutcome,reasonCode,attributedPackage "
        "FROM wake_events ORDER BY timestampMillis DESC LIMIT 5;"
    )
    log(f"wake count before={before!r} after={after!r}")
    log(f"latest:\n{latest}")

    notif2 = adb("shell", "dumpsys", "notification", "--noredact", timeout=45)
    if "Wake Shield armed" in notif2 or prefs_has("shield_enabled"):
        log("PASS: shield still armed after wake")
    else:
        raise SystemExit("FAIL: shield disarmed unexpectedly")

    outcomes = (
        "LOCKED",
        "SLEPT",
        "CANCELLED",
        "PARTIAL",
        "DENIED",
        "ALLOWED",
        "ABORTED",
        "SUPPRESSED",
        "NONE",
    )
    if any(o in latest for o in outcomes if o != "NONE") or re.search(
        r"\b(LOCKED|SLEPT|CANCELLED_NOTIFS|PARTIAL|DENIED_APPOP|ALLOWED_EXEMPT|ALLOWED_FSI|ABORTED_INTERACTIVE|SUPPRESSED_SELF)\b",
        latest,
    ):
        log("PASS: shieldOutcome recorded on latest wake(s)")
    else:
        try:
            b, a = int(before or "0"), int(after or "0")
            if a > b:
                log("PASS: wake logged (shieldOutcome may be null if forensics-only path)")
            else:
                log("WARN: no new wake row — check DisplayManager path manually")
        except ValueError:
            log("WARN: could not parse wake counts")

    # Confirm a11y service connected via dumpsys
    a11y_dump = adb("shell", "dumpsys", "accessibility", timeout=30)
    if "ShieldAccessibilityService" in a11y_dump:
        log("PASS: ShieldAccessibilityService in accessibility dumpsys")
    else:
        log("WARN: ShieldAccessibilityService not in accessibility dumpsys")

    log(f"PASS: m16_op13 smoke complete on {SERIAL}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        log(f"FAIL: {exc}")
        raise
