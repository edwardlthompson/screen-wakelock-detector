#!/usr/bin/env bash
# Grant Magisk su to the app (policy 2 = allow) via magisk.db.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "${SCRIPT_DIR}/grant-magisk-su.py" "${@:-com.screenwakelock.detector}"
