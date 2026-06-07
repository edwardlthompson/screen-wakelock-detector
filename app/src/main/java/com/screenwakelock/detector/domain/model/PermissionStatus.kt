package com.screenwakelock.detector.domain.model

enum class PermissionKind {
    NOTIFICATION_LISTENER,
    USAGE_STATS,
    POST_NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
}

data class PermissionStatus(
    val kind: PermissionKind,
    val granted: Boolean,
    val label: String,
    val description: String,
)
