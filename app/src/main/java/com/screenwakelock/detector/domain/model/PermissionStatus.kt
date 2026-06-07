package com.screenwakelock.detector.domain.model

enum class PermissionKind {
    RESTRICTED_SETTINGS,
    NOTIFICATION_LISTENER,
    USAGE_STATS,
    POST_NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
}

data class PermissionStatus(
    val kind: PermissionKind,
    val granted: Boolean,
    val label: String,
    val shortRationale: String,
    val description: String,
)
