#!/usr/bin/env python3
"""
Archive completed BUILD_PLAN tasks to COMPLETED.md.

Precondition: milestone smoke PASS must be recorded in docs/GATES.md.
Refuses to archive (exit 1) if smoke pass line is missing.

Usage:
  python scripts/archive-completed-tasks.py --milestone M0
  python scripts/archive-completed-tasks.py --milestone M2 --dry-run
  python scripts/archive-completed-tasks.py --all-completed
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUILD_PLAN = ROOT / "docs" / "BUILD_PLAN.md"
COMPLETED = ROOT / "docs" / "COMPLETED.md"
GATES = ROOT / "docs" / "GATES.md"

TASK_PATTERN = re.compile(
    r"^(\s*)- \[x\]\s+\[(AGENT|ADB|HUMAN)\](?:\s+\[PARALLEL-OK\])?\s+(.+)$",
    re.IGNORECASE,
)
UNCHECKED_PATTERN = re.compile(
    r"^(\s*)- \[ \]\s+\[(AGENT|ADB|HUMAN)\](?:\s+\[PARALLEL-OK\])?\s+(.+)$",
    re.IGNORECASE,
)
MILESTONE_HEADER = re.compile(r"^##\s+(M\d+)\s+—", re.IGNORECASE)
SMOKE_PASS_PATTERN = re.compile(
    r"Smoke\s+(M\d+):\s+PASS\s+",
    re.IGNORECASE,
)


def read_text(path: Path) -> str:
    if not path.exists():
        print(f"ERROR: missing {path}", file=sys.stderr)
        sys.exit(1)
    return path.read_text(encoding="utf-8")


def smoke_pass_recorded(gates_text: str, milestone: str) -> bool:
    """Return True if GATES.md contains 'Smoke M{N}: PASS ...' for milestone."""
    m = milestone.upper()
    if not m.startswith("M"):
        m = f"M{m}"
    for line in gates_text.splitlines():
        if SMOKE_PASS_PATTERN.search(line) and m.upper() in line.upper():
            return True
    # Also check smoke log table rows with PASS in milestone column context
    block = re.search(
        rf"\|\s*{re.escape(m)}\s*\|\s*PASS",
        gates_text,
        re.IGNORECASE,
    )
    return block is not None


def line_hash(line: str) -> str:
    return hashlib.sha256(line.strip().encode("utf-8")).hexdigest()[:16]


def parse_milestone_sections(text: str) -> dict[str, tuple[int, int]]:
    """Map milestone id -> (start_line, end_line) exclusive end."""
    lines = text.splitlines()
    headers: list[tuple[int, str]] = []
    for i, line in enumerate(lines):
        m = MILESTONE_HEADER.match(line)
        if m:
            headers.append((i, m.group(1).upper()))
    sections: dict[str, tuple[int, int]] = {}
    for idx, (start, mid) in enumerate(headers):
        end = headers[idx + 1][0] if idx + 1 < len(headers) else len(lines)
        sections[mid] = (start, end)
    return sections


def collect_completed_in_range(lines: list[str], start: int, end: int) -> list[tuple[int, str]]:
    found: list[tuple[int, str]] = []
    for i in range(start, end):
        m = TASK_PATTERN.match(lines[i])
        if m:
            found.append((i, lines[i]))
    return found


def already_archived(completed_text: str, task_line: str) -> bool:
    normalized = task_line.strip()
    h = line_hash(normalized)
    return h in completed_text or normalized in completed_text


def archive_milestone(
    milestone: str,
    dry_run: bool = False,
    force_smoke: bool = False,
) -> int:
    milestone = milestone.upper()
    if not milestone.startswith("M"):
        milestone = f"M{milestone}"

    build_text = read_text(BUILD_PLAN)
    gates_text = read_text(GATES)
    completed_text = read_text(COMPLETED) if COMPLETED.exists() else "# Completed Tasks\n\n"

    if not force_smoke and not smoke_pass_recorded(gates_text, milestone):
        print(
            f"ERROR: No smoke PASS for {milestone} in {GATES.relative_to(ROOT)}.\n"
            f"Record: Smoke {milestone}: PASS <ISO8601> <serial> <version>\n"
            f"See docs/ADB_TESTING.md and docs/GATES.md (Gate GSM).",
            file=sys.stderr,
        )
        return 1

    lines = build_text.splitlines()
    sections = parse_milestone_sections(build_text)
    if milestone not in sections:
        print(f"ERROR: milestone {milestone} not found in BUILD_PLAN", file=sys.stderr)
        return 1

    start, end = sections[milestone]
    completed_tasks = collect_completed_in_range(lines, start, end)
    if not completed_tasks:
        print(f"No completed [x] tasks in {milestone} section.")
        return 0

    to_archive: list[str] = []
    indices_to_remove: list[int] = []
    for idx, line in completed_tasks:
        if already_archived(completed_text, line):
            print(f"Skip (already archived): {line.strip()[:80]}...")
            indices_to_remove.append(idx)
            continue
        to_archive.append(line)
        indices_to_remove.append(idx)

    if not to_archive and not indices_to_remove:
        print("Nothing new to archive.")
        return 0

    today = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    header = f"\n## {today[:10]} — {milestone} (smoke passed)\n\n"
    appendix = header + "\n".join(to_archive) + "\n"

    if dry_run:
        print("DRY RUN — would append to COMPLETED.md:")
        print(appendix)
        print(f"Would remove {len(indices_to_remove)} line(s) from BUILD_PLAN.md")
        return 0

    if to_archive:
        with COMPLETED.open("a", encoding="utf-8") as f:
            f.write(appendix)

    new_lines = [ln for i, ln in enumerate(lines) if i not in set(indices_to_remove)]
    BUILD_PLAN.write_text("\n".join(new_lines) + ("\n" if new_lines else ""), encoding="utf-8")

    print(f"Archived {len(to_archive)} task(s) for {milestone}.")
    return 0


def archive_all_with_smoke(dry_run: bool = False) -> int:
    build_text = read_text(BUILD_PLAN)
    sections = parse_milestone_sections(build_text)
    rc = 0
    for mid in sorted(sections.keys(), key=lambda x: int(x[1:])):
        lines = build_text.splitlines()
        start, end = sections[mid]
        if collect_completed_in_range(lines, start, end):
            r = archive_milestone(mid, dry_run=dry_run)
            if r != 0:
                rc = r
            build_text = read_text(BUILD_PLAN)
            sections = parse_milestone_sections(build_text)
    return rc


def main() -> int:
    parser = argparse.ArgumentParser(description="Archive completed BUILD_PLAN tasks.")
    parser.add_argument(
        "--milestone",
        "-m",
        help="Milestone id, e.g. M0 or 0",
    )
    parser.add_argument(
        "--all-completed",
        action="store_true",
        help="Archive all milestones that have [x] tasks (each needs smoke PASS)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be archived without writing files",
    )
    parser.add_argument(
        "--force-smoke",
        action="store_true",
        help="Skip smoke PASS check (emergency only — not for normal use)",
    )
    args = parser.parse_args()

    if args.all_completed:
        return archive_all_with_smoke(dry_run=args.dry_run)
    if not args.milestone:
        parser.error("Specify --milestone M0 or --all-completed")
    return archive_milestone(
        args.milestone,
        dry_run=args.dry_run,
        force_smoke=args.force_smoke,
    )


if __name__ == "__main__":
    sys.exit(main())
