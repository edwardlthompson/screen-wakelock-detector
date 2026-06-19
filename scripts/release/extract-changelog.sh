#!/usr/bin/env bash
# Extract Keep a Changelog section for a version → stdout
set -euo pipefail

VERSION="${1:-}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

[[ -n "${VERSION}" ]] || { echo "Usage: extract-changelog.sh <version>" >&2; exit 1; }
VERSION="${VERSION#v}"

CHANGELOG="${ROOT}/CHANGELOG.md"
[[ -f "${CHANGELOG}" ]] || CHANGELOG="${ROOT}/docs/CHANGELOG.md"

[[ -f "${CHANGELOG}" ]] || { echo "See CHANGELOG.md"; exit 0; }

sed -n "/## \\[${VERSION}\\]/,/## \\[/p" "${CHANGELOG}" | head -n -1
