# shellcheck shell=bash
# Enable in-app root attribution on a rooted device (Magisk / su).
# Usage: ROOT_ENABLE_PACKAGE=com.screenwakelock.detector root_enable_app adb -s SERIAL

root_enable_app() {
  local adb_cmd=("$@")
  local pkg="${ROOT_ENABLE_PACKAGE:-com.screenwakelock.detector}"
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

  log_root() { echo "[root_enable] $*"; }

  bash "${script_dir}/grant-magisk-su.sh" "${pkg}" || true

  log_root "Enable root via debug automation deep link"
  "${adb_cmd[@]}" shell am start -a android.intent.action.VIEW \
    -d "screenwakelock://settings/root?automation=enable" -p "${pkg}" >/dev/null 2>&1 || true
  sleep 10
  log_root "If Magisk prompted on device, tap Grant for ${pkg}"
}
