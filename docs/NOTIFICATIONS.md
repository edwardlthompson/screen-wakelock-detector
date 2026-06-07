# Notifications and alerts

Alert copy templates, action buttons, and permission-callout variants for **Screen Wakelock Detector**.

Implementation: `WakeAlertNotifier` builds local notifications only — no network.

---

## Principles

1. **Name the offender** — `{AppName}` and `{ChannelName}` when known; never generic “screen woke”.
2. **Permission honesty** — Unknown wakes explain which grant would help and link to Settings row.
3. **Actionable** — Silence channel, open settings, or grant permission in one tap.
4. **Grouped** — Same package grouped to reduce spam (`setGroup`).

---

## Known offender (threshold alert)

**When:** Same app+channel exceeds user threshold in rolling window (e.g. 1 hour).

| Field | Template |
|-------|----------|
| **Title** | `{AppName} woke your screen` |
| **Body** | `{ChannelName} · {Count} times in the last hour — last at {Time}` |
| **Example title** | `Slack` |
| **Example body** | `Marketing · 5 times in the last hour — last at 11:42 PM` |

**Actions:**

- `Silence channel` → mute if API/OEM allows
- `Open settings` → channel or app notification settings

**Tap:** Opens quick-fix bottom sheet for that wake event.

**Style:** `BigTextStyle`, large icon = offender app icon when available.

---

## Single wake (immediate optional alert)

**When:** User enables “alert on every wake” (opt-in).

| Field | Template |
|-------|----------|
| **Title** | `Screen woke — {AppName}` |
| **Body** | `{ChannelName} notification · {ReasonCode friendly label}` |
| **Example** | Title: `Screen woke — Gmail` · Body: `Primary notification · Heads-up display` |

---

## Unknown offender — permission callouts

Inspect missing grants and use explicit copy:

| Missing grant | Title | Body | Action |
|---------------|-------|------|--------|
| Notification access | `Screen woke — app unknown` | `Your screen turned on at {Time}. Enable Notification access to identify which app caused it.` | `Grant access` → Permissions → Notification row |
| Usage access (notif OK) | `Screen woke — cause unclear` | `No matching notification found. Enable Usage access to improve detection.` | `Grant access` → Usage row |
| Battery restricted | `Wake may have been missed` | `Monitoring is limited while battery restricted. Some wakes may not be logged.` | `Fix battery` → Battery row |
| Multiple missing | `Screen woke — setup incomplete` | `{N} permissions off. Turn on permissions in Settings to identify wake sources.` | `Open Permissions` → Permissions screen |

**PendingIntent extras:** `wake_event_id`, `offender_package`, `channel_id`, or `missing_permission_key` for deep link.

**NavHost query:** `?highlight=notification_access` (etc.) scrolls and highlights row.

---

## Nighttime variant

**Window:** 11:00 PM – 6:00 AM local (configurable in Settings → Alerts).

- Prepend moon emoji in expanded text or use `errorContainer` accent
- Example: `3:12 AM — Facebook (Friend requests) woke your screen`

Insights tab also highlights nighttime wakes prominently (M5).

---

## In-app mirrors

Same rules apply to:

| Surface | Content |
|---------|---------|
| **Home last-wake card** | App icon, name, channel, time |
| **In-app banner** | When alerts enabled but POST_NOTIFICATIONS off |
| **Insights top offender** | `{App} · {Channel} · {Count} wakes` |
| **Unknown wake detail** | Banner with missing permission + Turn on link |

---

## Foreground service notification

**Channel:** Monitoring (low importance, ongoing)

| Field | Copy |
|-------|------|
| **Title** | `Monitoring screen wakes` |
| **Body** | `Tap to open wake history` |

Must remain visible while monitoring service runs (Android requirement).

---

## Quiet hours

Settings → Alerts → Quiet hours: suppress threshold alerts but continue logging.

In-app history and Insights still update.

---

## Testing

- `[AGENT]` unit tests: string builder given fixture `WakeEvent` + permission state
- `[ADB]` synthetic notification burst → threshold alert fires
- `[HUMAN]` copy review for tone and clarity

See [`ADB_TESTING.md`](ADB_TESTING.md) for smoke scripts.
