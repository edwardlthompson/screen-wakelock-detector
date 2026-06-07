# Privacy policy

**Screen Wakelock Detector** (`com.screenwakelock.detector`)

**Last updated:** 2026-06-06

---

## Summary

This app helps you understand **what wakes your phone screen**. All processing and storage happen **on your device**. We do not operate servers, accounts, analytics, or cloud sync for your wake data.

**Notification listener access reads notification metadata locally to attribute screen wakes — nothing leaves your device.**

---

## What we collect and store locally

| Data | Purpose | Retention |
|------|---------|-----------|
| Screen on/off timestamps | Wake history | Until you clear history or uninstall |
| Estimated wake duration | History display | Same |
| Attribution metadata | Show likely cause (app, channel, reason codes) | Same |
| Notification metadata | Correlate wakes to channels (package, channel ID, category, tag, post time) | Cached briefly; not message bodies by default |
| Confidence scores and reason codes | Transparency (“Why this app?”) | Same |
| Root diagnostic output (optional) | Parse wakelock holders when root enabled | Parsed fields only; raw dumpsys not persisted long-term |
| App settings | Preferences, alert thresholds, onboarding state | Same |

---

## What we do NOT collect

- Notification **message text** (bodies) — not stored by default
- Contacts, SMS, call logs, photos, or files from other apps
- Location, microphone, or camera data
- Advertising identifiers
- **Any network transmission** — the release app does not request `INTERNET` permission

---

## Permissions and why

See [`PERMISSIONS.md`](PERMISSIONS.md) for full detail. Summary:

- **Notification access** — identify which app/channel posted around wake time (metadata only)
- **Usage access** — fallback when no notification matches
- **Foreground service** — reliable background monitoring
- **Post notifications** — optional local alerts only
- **Battery exemption** — reduce missed wakes in Doze
- **Root (optional)** — local shell commands from fixed allowlist; parsed on device

---

## Data sharing

We do **not** sell, rent, or share your data with third parties. There is no backend.

**Export:** You may export or share individual events via Android share intent (user-initiated only). Exported content is under your control.

---

## Data retention and deletion

- **Clear history:** Settings → Data & privacy → Clear wake history
- **Uninstall:** Removes all app-private databases and preferences
- **Backup:** Wake database excluded from backup where configured — see [`SECURITY.md`](../SECURITY.md)

---

## Children

The app is not directed at children under 13. No personal accounts are created.

---

## Changes

Material privacy changes will be noted in [`CHANGELOG.md`](CHANGELOG.md) and this file’s “Last updated” date.

---

## Contact

Security or privacy questions: open a **confidential issue** on GitLab (when published) or see [`SECURITY.md`](../SECURITY.md).

---

## Open source

Source code is published under **Apache-2.0**. You can audit exactly what the app stores and how it behaves.
