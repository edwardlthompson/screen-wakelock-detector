# Shared ADB + device selection for smoke scripts.

resolve_smoke_adb() {
  if [[ -n "${ADB:-}" && "${ADB}" != "adb" ]]; then
    printf '%s\n' "${ADB}"
    return 0
  fi
  local sdk_adb
  sdk_adb="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
  if [[ -f "${sdk_adb}" ]]; then
    printf '%s\n' "${sdk_adb}"
    return 0
  fi
  sdk_adb="${HOME}/AppData/Local/Android/Sdk/platform-tools/adb.exe"
  if [[ -f "${sdk_adb}" ]]; then
    printf '%s\n' "${sdk_adb}"
    return 0
  fi
  if command -v adb >/dev/null 2>&1; then
    printf '%s\n' "$(command -v adb)"
    return 0
  fi
  printf '%s\n' "adb"
}

wait_for_smoke_device() {
  local adb_cmd="$1"
  local serial="${2:-}"
  local tries="${3:-20}"
  local i state
  for i in $(seq 1 "${tries}"); do
    if [[ -n "${serial}" ]]; then
      state="$("${adb_cmd}" -s "${serial}" get-state 2>/dev/null || true)"
      if [[ "${state}" == "device" ]]; then
        return 0
      fi
      if [[ "${serial}" == *:* ]]; then
        "${adb_cmd}" connect "${serial}" >/dev/null 2>&1 || true
      fi
    else
      if "${adb_cmd}" devices | awk 'NR>1 && $2=="device" {print $1; exit}' | grep -q .; then
        return 0
      fi
    fi
    sleep 1
  done
  return 1
}

pick_smoke_device() {
  local adb_cmd="${1:-adb}"
  if [[ -n "${SMOKE_DEVICE:-}" ]]; then
    wait_for_smoke_device "${adb_cmd}" "${SMOKE_DEVICE}" 15 || true
    echo "${SMOKE_DEVICE}"
    return 0
  fi
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    wait_for_smoke_device "${adb_cmd}" "${ANDROID_SERIAL}" 15 || true
    echo "${ANDROID_SERIAL}"
    return 0
  fi
  wait_for_smoke_device "${adb_cmd}" "" 15 || true
  local count
  count="$("${adb_cmd}" devices | awk 'NR>1 && $2=="device" {c++} END {print c+0}')"
  if [[ "${count}" -gt 1 ]]; then
    echo "[smoke] multiple adb devices; set SMOKE_DEVICE or ANDROID_SERIAL" >&2
    return 1
  fi
  "${adb_cmd}" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}
