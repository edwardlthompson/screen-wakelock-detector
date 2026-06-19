package com.screenwakelock.detector.domain.model

/**
 * Live notification visible in the status bar at wake time.
 * Used when cache correlation by [CachedNotification.postedAtMillis] misses ongoing alerts.
 */
data class ActiveNotificationSnapshot(
    val packageName: String,
    val channelId: String?,
    val channelName: String?,
    val category: String?,
    val importance: Int,
    val hasFullScreenIntent: Boolean,
    val hasTurnScreenOn: Boolean,
)
