package com.screenwakelock.detector.domain.model

enum class ReasonCode {
    NOTIFICATION_HEADS_UP,
    NOTIFICATION_RING,
    NOTIFICATION_FULL_SCREEN,
    NOTIFICATION_UNKNOWN,
    USAGE_STATS_FOREGROUND,
    USAGE_STATS_RECENT,
    ROOT_WAKELOCK,
    ROOT_WAKEUP_SOURCE,
    UNKNOWN,
    MULTIPLE_CANDIDATES,
    ;

    fun friendlyLabel(): String = when (this) {
        NOTIFICATION_HEADS_UP -> "Heads-up display"
        NOTIFICATION_RING -> "Ringing notification"
        NOTIFICATION_FULL_SCREEN -> "Full-screen intent"
        NOTIFICATION_UNKNOWN -> "Notification"
        USAGE_STATS_FOREGROUND -> "App was in foreground"
        USAGE_STATS_RECENT -> "Recently used app"
        ROOT_WAKELOCK -> "Active wakelock (root)"
        ROOT_WAKEUP_SOURCE -> "Kernel wakeup source (root)"
        UNKNOWN -> "Unknown cause"
        MULTIPLE_CANDIDATES -> "Multiple possible causes"
    }
}
