# Shared device selection for ADB smoke scripts.
pick_smoke_device() {
  local adb_cmd="${1:-adb}"
  if [[ -n "${SMOKE_DEVICE:-}" ]]; then
    echo "${SMOKE_DEVICE}"
    return 0
  fi
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    echo "${ANDROID_SERIAL}"
    return 0
  fi
  local count
  count="$("${adb_cmd}" devices | awk 'NR>1 && $2=="device" {c++} END {print c+0}')"
  if [[ "${count}" -gt 1 ]]; then
    echo "[smoke] multiple adb devices; set SMOKE_DEVICE or ANDROID_SERIAL" >&2
    return 1
  fi
  "${adb_cmd}" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}
