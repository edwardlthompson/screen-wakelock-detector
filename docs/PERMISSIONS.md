# Permissions

Technical rationale, system intent mapping, and OEM notes for **Screen Wakelock Detector**.

All special permissions are **optional for basic screen-on logging** except where noted. The app degrades gracefully when grants are missing.

---

## Permission summary

| Permission / capability | Android API | Required? | Purpose if granted | If denied |
|-------------------------|-------------|-----------|-------------------|-----------|
| **Notification listener** | `BIND_NOTIFICATION_LISTENER_SERVICE` | Strongly recommended | Match screen wakes to app + notification channel | Most wakes show Unknown or app-only |
| **Usage access** | `PACKAGE_USAGE_STATS` | Recommended | Fallback when no notification in correlation window | Lower confidence on non-notification wakes |
| **Foreground service** | `FOREGROUND_SERVICE` + type | Required for monitoring | Reliable background screen-on capture | Monitoring may not start |
| **Post notifications** | `POST_NOTIFICATIONS` (API 33+) | Optional | Threshold alerts naming app + channel | Alerts in-app only |
| **Battery unrestricted** | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Recommended | Reliable capture during Doze/deep sleep | Missed wakes when aggressively optimized |
| **Root (`su`)** | Runtime via libsu | Optional | Live wakelock holder from dumpsys parsers | Root UI grayed; non-root attribution only |

**Not used:** `INTERNET`, location, contacts, SMS, microphone, camera.

---

## Notification access

**System setting:** Settings → Apps → Special app access → Notification access

**Intent:** `Settings.ACTION_NOTIFICATION_LISTENER_SERVICE` / `ACTION_NOTIFICATION_LISTENER_SETTINGS`

**What we read (metadata only):**

- Posting package name
- Notification channel ID and importance
- Category, tag, group key
- Post time
- Flags relevant to heads-up / full-screen intent

**What we never read or store:**

- Notification message body text (by default)
- Contact names from messaging apps
- Images / media attachments

**Settings switch behavior:**

- ON → open notification listener settings; refresh on resume
- OFF → dialog directing user to revoke in system settings

### Sideloaded apps (Android 13+ restricted settings)

If the app was installed from a **GitHub APK, browser, or Files app** (not Play Store / F-Droid / session installer), Android blocks Notification access and Usage access until the user enables **Allow restricted settings**:

**Install detection:** Play Store (`com.android.vending`) and F-Droid (`org.fdroid.fdroid`) are trusted. Browser downloads use `com.android.packageinstaller` with originating package Firefox/Chrome/Files — the app treats these as sideloads and shows the restricted-settings chip.

1. Try enabling Notification access once (system shows “Restricted setting” — tap OK).
2. **Settings → Apps → Screen Wakelock Detector → App info → menu (⋮) → Allow restricted settings** (confirm PIN).
3. Grant **Notification access** and **Usage access** from Special app access.

The in-app permission rows on onboarding and Settings → Permissions open the best Settings screen for the device. **`SettingsGuideProvider`** tries an ordered intent chain per permission; if none resolve, **`PermissionStepsDialog`** shows numbered manual steps.

**LineageOS / AOSP:** App info → menu (⋮) → **Allow restricted settings** (confirm PIN).

**OnePlus / OxygenOS:** Tap Grant on Notification access — when the **Restricted setting** system dialog appears, tap **Allow**. AppOps may still read `default`; the in-app chip turns green once Notification or Usage access is enabled. If blocked, use App info → ⋮ → Allow restricted settings.

**Installer workaround:** If ⋮ menu item is missing, reinstall via session install (e.g. SAI) — see in-app dialog link.

**OnePlus / OxygenOS 15:** May need SAI session reinstall (same workaround dialog).

---

## Usage access

**System setting:** Settings → Apps → Special app access → Usage access

**Intent:** `Settings.ACTION_USAGE_ACCESS_SETTINGS`

**Purpose:** When no notification matches the wake window, check which app was foreground or recently active.

**Skip consequence:** Non-notification wakes (alarms, some wakelocks) may remain Unknown.

---

## Battery optimization

**Intent (stock):** `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with `package:` URI

**Purpose:** Prevent Android from killing the foreground monitoring service during deep sleep.

### OEM paths

| OEM | Path notes |
|-----|------------|
| **Google Pixel / stock Android** | Settings → Apps → Screen Wakelock Detector → Battery → Unrestricted |
| **Samsung One UI** | Settings → Apps → Screen Wakelock Detector → Battery → Unrestricted; also check Sleeping apps list |
| **Xiaomi MIUI/HyperOS** | Security app → Permissions → Autostart ON; Battery → No restrictions |
| **OnePlus OxygenOS** | Settings → Battery → Battery optimization → Don't optimize |
| **Oppo/ColorOS** | Settings → Battery → App battery management → Allow background activity |

Document device-specific quirks in [`AGENT_MEMORY.md`](AGENT_MEMORY.md) after testing.

### Doze survival flow (onboarding + Settings)

Monitoring uses a **foreground service** with a persistent notification. On aggressive OEM battery savers, Android may still defer or kill background work during Doze.

**Recommended user path:**

1. Complete onboarding through **Privacy** (monitoring can start before optional permissions).
2. On the **Permissions** step, tap **Battery unrestricted** (or open Settings → Battery).
3. On stock Android / Pixel: Settings → Apps → Screen Wakelock Detector → Battery → **Unrestricted**.
4. On Samsung / Xiaomi / OnePlus: also disable sleeping apps / enable autostart (see OEM table above).
5. Confirm the **Monitoring active** notification stays visible after locking the device overnight.

**If wakes are missed:** check that the FGS notification is present, battery is unrestricted, and the app was not force-stopped. Reboot after granting battery exemption on some OEMs.

See onboarding Permissions step and [`ADB_TESTING.md`](ADB_TESTING.md) for smoke validation.

---

## Post notifications (API 33+)

**Runtime permission:** `POST_NOTIFICATIONS`

**Purpose:** Optional local alerts when an app/channel wakes the screen repeatedly (see [`NOTIFICATIONS.md`](NOTIFICATIONS.md)).

**Not required** for core wake logging.

---

## Root access

See [`ROOT.md`](ROOT.md). Root is **never required** for core functionality.

- One-time `su` grant via Magisk, KernelSU, APatch, etc.
- All tooling ships inside the APK (libsu + parsers)
- No Shizuku, Magisk modules, or companion apps

---

## Settings → Permissions mapping

| UI row | Switch reflects | Grant action | Revoke guidance |
|--------|-----------------|--------------|-----------------|
| Notification access | `NotificationManagerCompat.getEnabledListenerPackages()` | Open listener settings | Dialog → same settings |
| Usage access | `AppOpsManager` / UsageStatsManager check | Open usage access settings | Dialog → usage settings |
| Battery unrestricted | `PowerManager.isIgnoringBatteryOptimizations()` | Battery exemption intent | Dialog → app battery settings |
| Alert notifications | `POST_NOTIFICATIONS` granted (API 33+) | Runtime request | App notification settings |
| Root status | Read-only | Link to Settings → Root | N/A |

Deep links from Home chips and alerts use `?highlight=<permission_key>` on Settings NavHost.

---

## Security notes

- Notification listener is system-bound; our service is not exported to third-party apps.
- Stored notification fields are metadata-only — see [`SECURITY.md`](../SECURITY.md) and [`PRIVACY.md`](PRIVACY.md).
