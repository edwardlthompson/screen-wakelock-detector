#!/usr/bin/env python3
"""
Bump fdroid/metadata/com.screenwakelock.detector.yml from Gradle versionName/versionCode.

Usage:
  python scripts/fdroid/bump-metadata.py
  python scripts/fdroid/bump-metadata.py --version-name 1.0.0 --version-code 10
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
METADATA = ROOT / "fdroid" / "metadata" / "com.screenwakelock.detector.yml"
GRADLE = ROOT / "app" / "build.gradle.kts"


def parse_gradle_versions() -> tuple[str, int]:
    if not GRADLE.exists():
        return "0.1.0", 1
    text = GRADLE.read_text(encoding="utf-8")
    name_m = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code_m = re.search(r'versionCode\s*=\s*(\d+)', text)
    name = name_m.group(1) if name_m else "0.1.0"
    code = int(code_m.group(1)) if code_m else 1
    return name, code


def bump_metadata(version_name: str, version_code: int, commit: str | None) -> None:
    if not METADATA.exists():
        print(f"ERROR: {METADATA} not found", file=sys.stderr)
        sys.exit(1)

    text = METADATA.read_text(encoding="utf-8")
    commit = commit or "master"

    text = re.sub(
        r"^CurrentVersion:\s*.+$",
        f"CurrentVersion: {version_name}",
        text,
        flags=re.MULTILINE,
    )
    text = re.sub(
        r"^CurrentVersionCode:\s*.+$",
        f"CurrentVersionCode: {version_code}",
        text,
        flags=re.MULTILINE,
    )

    build_block = f"""
  - versionName: '{version_name}'
    versionCode: {version_code}
    commit: {commit}
    subdir: app
    sudo:
      - apt-get update
      - apt-get install -y openjdk-17-jdk-headless
    gradle:
      - assembleRelease
"""

    if re.search(r"^Builds:\s*$", text, re.MULTILINE):
        # Append new build entry if versionCode not present
        if f"versionCode: {version_code}" not in text:
            text = re.sub(r"(^Builds:\s*$)", r"\1" + build_block, text, count=1, flags=re.MULTILINE)
    else:
        text += f"\nBuilds:{build_block}\n"

    METADATA.write_text(text, encoding="utf-8")
    print(f"Bumped metadata → {version_name} ({version_code})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-name")
    parser.add_argument("--version-code", type=int)
    parser.add_argument("--commit", default=None, help="Git commit ref for Builds block")
    args = parser.parse_args()

    g_name, g_code = parse_gradle_versions()
    name = args.version_name or g_name
    code = args.version_code or g_code
    bump_metadata(name, code, args.commit)


if __name__ == "__main__":
    main()
