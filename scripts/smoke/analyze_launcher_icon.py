#!/usr/bin/env python3
"""Analyze launcher screenshot for adaptive icon legibility."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageStat
except ImportError as exc:  # pragma: no cover
    print(f"FAIL: Pillow required ({exc})", file=sys.stderr)
    sys.exit(2)

# ic_launcher_background #004D57
BRAND_BG = (0, 77, 87)
BRAND_BG_TOLERANCE = 55


def luma(rgb: tuple[int, ...]) -> float:
    r, g, b = rgb[:3]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def color_dist(a: tuple[int, ...], b: tuple[int, int, int]) -> float:
    return sum((int(a[i]) - b[i]) ** 2 for i in range(3)) ** 0.5


def pixel_rows(img: Image.Image) -> list[tuple[int, int, int, int]]:
    rgba = img.convert("RGBA")
    return list(rgba.get_flattened_data())


def estimate_background(img: Image.Image) -> float:
    w, h = img.size
    samples = []
    for x, y in (
        (2, 2),
        (w - 3, 2),
        (2, h - 3),
        (w - 3, h - 3),
        (w // 2, 2),
        (w // 2, h - 3),
    ):
        samples.append(luma(img.getpixel((x, y))))
    return sum(samples) / len(samples)


def find_icon_by_brand_color(screenshot: Path) -> tuple[int, int, int, int] | None:
    img = Image.open(screenshot).convert("RGBA")
    w, h = img.size
    mask = [[False] * w for _ in range(h)]
    hits: list[tuple[int, int]] = []
    for y in range(h):
        for x in range(w):
            if color_dist(img.getpixel((x, y)), BRAND_BG) <= BRAND_BG_TOLERANCE:
                mask[y][x] = True
                hits.append((x, y))
    if len(hits) < 80:
        return None

    xs = [p[0] for p in hits]
    ys = [p[1] for p in hits]
    x1, x2 = min(xs), max(xs)
    y1, y2 = min(ys), max(ys)
    # Expand to square icon cell
    size = max(x2 - x1, y2 - y1)
    pad = max(4, size // 8)
    size = int(size * 1.15) + pad * 2
    cx = (x1 + x2) // 2
    cy = (y1 + y2) // 2
    left = max(0, cx - size // 2)
    top = max(0, cy - size // 2)
    right = min(w, left + size)
    bottom = min(h, top + size)
    left = max(0, right - size)
    top = max(0, bottom - size)
    return left, top, right, bottom


def crop_above_label(
    screenshot: Path,
    bounds: tuple[int, int, int, int],
) -> Image.Image:
    x1, y1, x2, y2 = bounds
    label_w = x2 - x1
    label_h = y2 - y1
    size = max(label_w, int(label_h * 2.8), 96)
    cx = (x1 + x2) // 2
    top = max(0, y1 - size - int(label_h * 0.35))
    left = max(0, cx - size // 2)
    img = Image.open(screenshot)
    right = min(img.width, left + size)
    bottom = min(img.height, top + size)
    left = max(0, right - size)
    top = max(0, bottom - size)
    return img.crop((left, top, right, bottom))


def analyze_crop(img: Image.Image, label: str) -> dict:
    img = img.convert("RGBA")
    w, h = img.size
    if min(w, h) < 32:
        raise ValueError(f"{label}: crop too small ({w}x{h})")

    rows = pixel_rows(img)
    lumas = [luma(px) for px in rows if px[3] > 128]
    if not lumas:
        raise ValueError(f"{label}: crop fully transparent")

    contrast = max(lumas) - min(lumas)
    stdev = ImageStat.Stat(img.convert("L")).stddev[0]
    bg_luma = estimate_background(img)
    edge_threshold = 16.0

    cx, cy = w / 2, h / 2
    radius = min(w, h) * 0.48
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=255)

    safe_r = min(w, h) * 0.305
    safe = Image.new("L", (w, h), 0)
    sdraw = ImageDraw.Draw(safe)
    sdraw.ellipse((cx - safe_r, cy - safe_r, cx + safe_r, cy + safe_r), fill=255)

    inset = int(min(w, h) * 0.075)
    square = Image.new("L", (w, h), 0)
    sqdraw = ImageDraw.Draw(square)
    sqdraw.rectangle((inset, inset, w - inset, h - inset), fill=255)

    def structured_ratio(m: Image.Image) -> float:
        mpx = m.load()
        structured = 0
        total = 0
        for y in range(h):
            for x in range(w):
                if mpx[x, y] == 0:
                    continue
                total += 1
                px = rows[y * w + x]
                if abs(luma(px) - bg_luma) >= edge_threshold:
                    structured += 1
        return structured / total if total else 0.0

    return {
        "label": label,
        "size": [w, h],
        "luma_contrast": round(contrast, 2),
        "luma_stdev": round(stdev, 2),
        "round_mask_structured_ratio": round(structured_ratio(mask), 3),
        "safe_zone_structured_ratio": round(structured_ratio(safe), 3),
        "square_mask_structured_ratio": round(structured_ratio(square), 3),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("screenshot", type=Path)
    parser.add_argument("--bounds", help="label bounds x1,y1,x2,y2")
    parser.add_argument("--auto-color", action="store_true", help="find #004D57 icon blob")
    parser.add_argument("--out-crop", type=Path, required=True)
    parser.add_argument("--out-json", type=Path, required=True)
    parser.add_argument("--min-contrast", type=float, default=30.0)
    parser.add_argument("--min-stdev", type=float, default=10.0)
    parser.add_argument("--min-round-ratio", type=float, default=0.05)
    parser.add_argument("--min-safe-ratio", type=float, default=0.04)
    args = parser.parse_args()

    if args.auto_color:
        found = find_icon_by_brand_color(args.screenshot)
        if not found:
            print("FAIL: brand-color icon blob not found in screenshot", file=sys.stderr)
            return 1
        crop = Image.open(args.screenshot).crop(found)
    elif args.bounds:
        parts = [int(p) for p in args.bounds.split(",")]
        if len(parts) != 4:
            print("FAIL: bounds must be x1,y1,x2,y2", file=sys.stderr)
            return 1
        crop = crop_above_label(args.screenshot, tuple(parts))
    else:
        print("FAIL: provide --bounds or --auto-color", file=sys.stderr)
        return 1

    crop.save(args.out_crop)
    metrics = analyze_crop(crop, args.screenshot.name)
    args.out_json.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    errors = []
    if metrics["luma_contrast"] < args.min_contrast:
        errors.append(f"luma contrast {metrics['luma_contrast']} < {args.min_contrast}")
    if metrics["luma_stdev"] < args.min_stdev:
        errors.append(f"luma stdev {metrics['luma_stdev']} < {args.min_stdev}")
    if metrics["round_mask_structured_ratio"] < args.min_round_ratio:
        errors.append(
            f"round mask ratio {metrics['round_mask_structured_ratio']} < {args.min_round_ratio}"
        )
    if metrics["safe_zone_structured_ratio"] < args.min_safe_ratio:
        errors.append(
            f"safe zone ratio {metrics['safe_zone_structured_ratio']} < {args.min_safe_ratio}"
        )

    if errors:
        for e in errors:
            print(f"FAIL: {e}", file=sys.stderr)
        return 1

    print(
        f"OK   contrast={metrics['luma_contrast']} "
        f"round={metrics['round_mask_structured_ratio']} "
        f"safe={metrics['safe_zone_structured_ratio']}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
