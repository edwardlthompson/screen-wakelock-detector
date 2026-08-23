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

    fun filterGroup(): ReasonFilterGroup = when (this) {
        NOTIFICATION_HEADS_UP,
        NOTIFICATION_RING,
        NOTIFICATION_FULL_SCREEN,
        NOTIFICATION_UNKNOWN,
        -> ReasonFilterGroup.NOTIFICATIONS

        USAGE_STATS_FOREGROUND,
        USAGE_STATS_RECENT,
        -> ReasonFilterGroup.USAGE

        ROOT_WAKELOCK,
        ROOT_WAKEUP_SOURCE,
        -> ReasonFilterGroup.ROOT

        UNKNOWN,
        MULTIPLE_CANDIDATES,
        -> ReasonFilterGroup.UNKNOWN
    }

    fun explainer(): String = when (this) {
        NOTIFICATION_HEADS_UP ->
            "A heads-up notification posted in the correlation window."
        NOTIFICATION_RING ->
            "A ringing or incoming-call style notification posted."
        NOTIFICATION_FULL_SCREEN ->
            "A full-screen intent took the display (alarm, call, or similar)."
        NOTIFICATION_UNKNOWN ->
            "A notification from this app posted, without a more specific kind."
        USAGE_STATS_FOREGROUND ->
            "Usage stats said this app was in the foreground when the screen came on."
        USAGE_STATS_RECENT ->
            "Usage stats said this app was used just before the wake."
        ROOT_WAKELOCK ->
            "Root dumpsys showed an active wakelock for this app."
        ROOT_WAKEUP_SOURCE ->
            "The kernel wakeup_sources file named this holder."
        UNKNOWN ->
            "No notification, usage, or wakelock signal matched this wake."
        MULTIPLE_CANDIDATES ->
            "More than one app looked equally likely — pick one if you know."
    }
}

enum class ReasonFilterGroup(val label: String) {
    NOTIFICATIONS("Notifications"),
    USAGE("Usage stats"),
    ROOT("Root"),
    UNKNOWN("Unknown"),
}
