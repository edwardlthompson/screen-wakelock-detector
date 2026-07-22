#!/usr/bin/env python3
import base64
import subprocess
import sys
from pathlib import Path

serial = sys.argv[1]
pkg = "com.screenwakelock.detector"
prefs = Path("_m16_settings.preferences_pb").read_bytes()
b64 = base64.b64encode(prefs).decode("ascii")
subprocess.run(["adb", "-s", serial, "shell", "am", "force-stop", pkg], check=False)
script = (
    "mkdir -p files/datastore; "
    f"echo {b64} | base64 -d > files/datastore/settings.preferences_pb; "
    "ls -l files/datastore/settings.preferences_pb; "
    "wc -c files/datastore/settings.preferences_pb"
)
r = subprocess.run(
    ["adb", "-s", serial, "shell", "run-as", pkg, "sh", "-c", script],
    capture_output=True,
    text=True,
)
print(r.stdout)
print(r.stderr, file=sys.stderr)
sys.exit(0 if r.returncode == 0 else r.returncode)
