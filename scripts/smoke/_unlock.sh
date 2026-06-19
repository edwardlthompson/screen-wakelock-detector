# shellcheck shell=bash
# Unlock device for UI smokes. Set SMOKE_PIN in environment or .env (gitignored).
# Usage: smoke_unlock adb -s SERIAL

smoke_unlock() {
  local adb_cmd=("$@")
  local pin="${SMOKE_PIN:-}"

  if [[ -z "${pin}" && -f "${ROOT:-.}/.env" ]]; then
    pin="$(grep -E '^SMOKE_PIN=' "${ROOT}/.env" 2>/dev/null | cut -d= -f2- | tr -d '\r\"' || true)"
  fi

  "${adb_cmd[@]}" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  "${adb_cmd[@]}" shell wm dismiss-keyguard 2>/dev/null || true
  sleep 1
  "${adb_cmd[@]}" shell input swipe 720 2400 720 800 350 2>/dev/null || true
  sleep 1

  local ui
  ui="$("${adb_cmd[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
  if echo "${ui}" | grep -q 'Enter PIN\|Enter password\|Pattern lock'; then
    if [[ -n "${pin}" ]]; then
      "${adb_cmd[@]}" shell input text "${pin}" 2>/dev/null || true
      sleep 0.5
      "${adb_cmd[@]}" shell input keyevent KEYCODE_ENTER 2>/dev/null || true
      sleep 2
    else
      echo "[smoke_unlock] Device locked — set SMOKE_PIN or unlock manually" >&2
      return 1
    fi
  fi
  return 0
}

smoke_assert_unlocked() {
  local adb_cmd=("$@")
  local ui
  ui="$("${adb_cmd[@]}" exec-out uiautomator dump /dev/stdout 2>/dev/null || true)"
  if echo "${ui}" | grep -q 'Enter PIN\|Enter password\|Pattern lock'; then
    echo "[smoke_unlock] Still on lock screen after unlock attempt" >&2
    return 1
  fi
  return 0
}
