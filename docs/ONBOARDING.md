# Onboarding

Canonical copy and flow for **Screen Wakelock Detector** first-run experience and F-Droid store screenshots.

Cross-links: [`PERMISSIONS.md`](PERMISSIONS.md) · [`PRIVACY.md`](PRIVACY.md) · [`NOTIFICATIONS.md`](NOTIFICATIONS.md)

---

## Design principles

1. **Explain → then ask** — What we do / Why we need this / What we never access, then Grant.
2. **Progressive disclosure** — Welcome and how-it-works before any system settings intent.
3. **Honest skip paths** — Every step offers “Skip for now” with plain consequences.
4. **Never dead-end** — Partial permissions still allow monitoring and history.
5. **Re-enter anytime** — Settings → Permissions + Home permission chips.

---

## Flow overview

```
Welcome → How it works (4 steps) → Privacy → Home (monitoring starts)
    → Permissions hub (on demand) → per-permission screens → Verify setup → Done
```

---

## Step 0 — Welcome

**Headline:** Find out what keeps waking your screen

**Subtext:** We log every screen-on, show which app or channel likely caused it, and help you fix it in one tap. Everything stays on your device.

**Primary CTA:** Continue

**Secondary:** _(none on first screen)_

---

## Step 1 — How it works

Four illustrated cards (horizontal pager or vertical scroll):

| # | Title | Body |
|---|-------|------|
| 1 | **Detect** | We listen for screen-on events in the background. |
| 2 | **Identify** | We match each wake to a notification, channel, or wakelock when possible. |
| 3 | **Show** | You get a timestamped history with confidence and an explanation. |
| 4 | **Fix** | Jump to that app’s notification settings or mute the channel. |

**Primary CTA:** Continue

---

## Step 2 — Privacy

**Headline:** Your data stays on your phone

**Bullets:**

- Wake history stored locally in an app-private database
- No cloud upload, no account, no analytics telemetry
- You can clear history anytime in Settings
- Notification **metadata** only — not message contents (see [Privacy policy](PRIVACY.md))

**Primary CTA:** Start monitoring

**Link:** Read full privacy policy

---

## Step 3 — Home (soft landing)

- Monitoring starts immediately (screen-on timestamps)
- Permission progress ring: e.g. “2 of 4 recommended permissions granted”
- Dismissible banner: “Complete setup for accurate attribution →”

---

## Permission steps (one screen each)

Template: icon, title, three `ListItem` rationale rows, **Grant** button, **Skip for now** link, supporting text for skip consequence.

### 1 — Notification access

**Required?** Strongly recommended

| Block | Copy |
|-------|------|
| **What we do** | See which notification turned your screen on — down to the channel (e.g. “Marketing” vs “Messages”). |
| **Why we need this** | Channel-level attribution is the fastest way to find repeat offenders. |
| **What we never access** | Message text, photos, or contact names from notifications. |

**Skip consequence:** Most wakes will show as Unknown or app-only.

**Grant opens:** Notification listener settings

---

### 2 — Usage access

**Required?** Recommended

| Block | Copy |
|-------|------|
| **What we do** | Check which app was active right before the screen lit up. |
| **Why we need this** | Helps when a wake wasn’t caused by a visible notification. |
| **What we never access** | Full browsing history or keystrokes — only recent foreground app hints. |

**Skip consequence:** Lower confidence on non-notification wakes.

**Grant opens:** Usage access settings

---

### 3 — Battery unrestricted

**Required?** Recommended

| Block | Copy |
|-------|------|
| **What we do** | Keep background monitoring reliable during deep sleep. |
| **Why we need this** | Android may stop background work to save battery. |
| **What we never access** | Nothing extra — this only affects our monitoring service. |

**Skip consequence:** You may miss wakes when the phone is in deep sleep.

**Grant opens:** Battery exemption intent (OEM paths in [`PERMISSIONS.md`](PERMISSIONS.md))

---

### 4 — Post notifications (API 33+)

**Required?** Optional

| Block | Copy |
|-------|------|
| **What we do** | Send optional alerts like “App X woke your screen 5 times tonight.” |
| **Why we need this** | So you don’t have to open the app to notice repeat offenders. |
| **What we never access** | N/A — alerts are generated locally from your wake log. |

**Skip consequence:** Alerts stay in-app only.

**Grant opens:** Runtime `POST_NOTIFICATIONS` request

---

### 5 — Root (informational)

**Required?** Optional

| Block | Copy |
|-------|------|
| **What we do** | Read wakelock data via an in-app root shell for deeper accuracy. |
| **Why we need this** | Some wakes aren’t tied to notifications — root reveals kernel wakelock holders. |
| **What we never access** | Nothing outside fixed diagnostic commands — see [`ROOT.md`](ROOT.md). |

**Copy highlight:** Everything is built into this app — no extra modules or plugins. Enable later in Settings → Root.

**No grant button** — informational only; link to Settings → Root

---

## Verify setup (final onboarding)

Summary checklist with live status:

- Monitoring active
- Notification access: granted / missing
- Usage access: granted / missing
- Battery: unrestricted / restricted
- Expected accuracy: **High** / **Medium** / **Basic**

**Primary CTA:** Start monitoring  
**Secondary:** Fix missing permissions (jumps to first incomplete step)

---

## Ongoing UX

- **Home permission chips** — tap → scroll to Settings → Permissions row
- **First Unknown wake nudge** — names missing permission + Turn on link
- **Replay wizard** — Settings → Permissions → “Run setup again”

---

## F-Droid screenshot order

Recommended store listing slides (light + dark + dynamic color where possible):

1. Welcome hero — “Find out what keeps waking your screen”
2. How it works — 4-step illustration
3. Privacy — local-only bullets
4. Home with wake history (sample data)
5. Wake detail — app, channel, confidence bar
6. Notification access grant screen (system settings overlay optional)
7. Settings → Permissions hub
8. Insights tab (M5+)
9. Root settings — grayed vs enabled (M3+)

Place PNGs in `fastlane/metadata/android/en-US/images/phoneScreenshots/`.
