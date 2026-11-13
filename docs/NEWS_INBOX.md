# News Inbox

Auto-collected inbox of news, upstream updates, security fixes, and ideas relevant to this repo, appended daily by News Bot.
A Cursor planning agent should review these items and fold the useful ones into BUILD_PLAN; entries are append-only, newest at the bottom.


## 2026-11-13

**News and updates**

- Android 17 QPR2 stable: lock-screen widget editing, status-bar notification filters, share-sheet changes, brightness override. [source](https://www.androidauthority.com/android-17-qpr2-stable-3733645/)
- Kotlin 2.5.0-RC released (fixes a reified Enum<T> inference regression); Gradle 9.9.1 / 8.14.7 patch releases. [source](https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC)

**Tasks for the planning agent**

- [ ] Check detection with Android 17 QPR2 brightness override — Brightness override and QPR2 changes may alter screen-wake attribution. Label: `[ADB]` · Priority: P3 · https://www.androidauthority.com/android-17-qpr2-stable-3733645/
- [ ] Plan dependency refresh (Compose BOM 2024.12, SDK 35) and Kotlin 2.5 — The app is far behind the rest of the portfolio's Android stack. Label: `[AGENT]` · Priority: P2 · https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC
