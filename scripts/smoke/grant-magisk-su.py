#!/usr/bin/env python3
"""Grant Magisk su to the app (policy 2 = allow) via magisk.db."""
import os
import subprocess
import sys
import tempfile

device = os.environ.get("ANDROID_SERIAL", "b5214fc6")
pkg = sys.argv[1] if len(sys.argv) > 1 else "com.screenwakelock.detector"
adb = os.environ.get("ADB", "adb")


def run(args: list[str]) -> str:
    return subprocess.check_output([adb, "-s", device, *args], text=True).strip()


uid = run(["shell", "su", "-c", f"stat -c %u /data/user/0/{pkg}"])
sql = f"INSERT OR REPLACE INTO policies VALUES({uid},2,0,1,1);\n"
print(f"Grant Magisk su: {pkg} uid={uid}")

with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as f:
    f.write(sql)
    tmp = f.name

try:
    subprocess.check_call([adb, "-s", device, "push", tmp, "/sdcard/magisk-grant.sql"])
    subprocess.check_call(
        [
            adb,
            "-s",
            device,
            "shell",
            "su",
            "-c",
            "sqlite3 /data/adb/magisk.db < /sdcard/magisk-grant.sql",
        ],
    )
    print("Magisk policy granted")
finally:
    os.unlink(tmp)
