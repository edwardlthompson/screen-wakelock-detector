#!/usr/bin/env python3
"""Write a 1024x500 F-Droid featureGraphic.png (no third-party deps)."""
from __future__ import annotations

import struct
import zlib
from pathlib import Path


def _chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def write_png(path: Path, width: int, height: int, rgb_rows: list[bytes]) -> None:
    raw = b"".join(b"\x00" + row for row in rgb_rows)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + _chunk(b"IDAT", zlib.compress(raw, 9))
        + _chunk(b"IEND", b""),
    )


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    dest = root / "fastlane" / "metadata" / "android" / "en-US" / "images" / "featureGraphic.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    width, height = 1024, 500
    rows: list[bytes] = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            xt = x / width
            yt = y / height
            r = int(8 + 18 * xt)
            g = int(42 + 36 * (1 - yt))
            b = int(58 + 70 * xt)
            phone = 72 <= x <= 248 and 70 <= y <= 430
            if phone:
                r, g, b = 236, 242, 248
            elif 88 <= x <= 232 and 96 <= y <= 404:
                r, g, b = 16, 28, 36
            elif 400 <= x <= 960 and 170 <= y <= 230:
                r, g, b = 232, 244, 248
            elif 400 <= x <= 820 and 250 <= y <= 290:
                r, g, b = 180, 210, 220
            row += bytes((r, g, b))
        rows.append(bytes(row))
    write_png(dest, width, height, rows)
    print(f"Wrote {dest} ({dest.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
