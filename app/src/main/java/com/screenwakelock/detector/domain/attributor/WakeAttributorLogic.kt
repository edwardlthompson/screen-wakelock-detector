package com.screenwakelock.detector.domain.attributor

import android.app.Notification
import android.app.NotificationManager
import com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot
import com.screenwakelock.detector.domain.model.CachedNotification
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeCandidate
import com.screenwakelock.detector.root.RootSnapshot

fun notificationReasonCode(
    category: String?,
    importance: Int,
    hasFullScreenIntent: Boolean = false,
    hasTurnScreenOn: Boolean = false,
): ReasonCode = when {
    hasFullScreenIntent || hasTurnScreenOn -> ReasonCode.NOTIFICATION_FULL_SCREEN
    category == Notification.CATEGORY_CALL -> ReasonCode.NOTIFICATION_RING
    category == Notification.CATEGORY_ALARM -> ReasonCode.NOTIFICATION_FULL_SCREEN
    importance >= NotificationManager.IMPORTANCE_HIGH -> ReasonCode.NOTIFICATION_HEADS_UP
    else -> ReasonCode.NOTIFICATION_UNKNOWN
}

fun isWakeLikelyNotification(
    category: String?,
    importance: Int,
    hasFullScreenIntent: Boolean,
    hasTurnScreenOn: Boolean,
): Boolean = hasFullScreenIntent ||
    hasTurnScreenOn ||
    importance >= NotificationManager.IMPORTANCE_HIGH ||
    category == Notification.CATEGORY_CALL ||
    category == Notification.CATEGORY_ALARM

fun cachedNotificationCandidates(
    notifications: List<CachedNotification>,
    screenOnMillis: Long,
    correlationWindowMs: Long,
    resolveLabel: (String) -> String?,
): List<WakeCandidate> = notifications.map { notif ->
    val reason = notificationReasonCode(notif.category, notif.importance)
    val proximity = 1f - (
        kotlin.math.abs(notif.postedAtMillis - screenOnMillis).toFloat() /
            correlationWindowMs
        ).coerceIn(0f, 1f)
    WakeCandidate(
        packageName = notif.packageName,
        appLabel = resolveLabel(notif.packageName),
        channelId = notif.channelId,
        channelName = notif.channelName,
        reasonCode = reason,
        confidence = 0.5f + proximity * 0.45f,
    )
}.sortedByDescending { it.confidence }

fun activeNotificationCandidates(
    snapshots: List<ActiveNotificationSnapshot>,
    existing: List<WakeCandidate>,
    resolveLabel: (String) -> String?,
): List<WakeCandidate> {
    val existingKeys = existing.map { it.packageName to it.channelId }.toSet()
    return snapshots
        .filter { snap ->
            isWakeLikelyNotification(
                snap.category,
                snap.importance,
                snap.hasFullScreenIntent,
                snap.hasTurnScreenOn,
            )
        }
        .filter { snap -> (snap.packageName to snap.channelId) !in existingKeys }
        .map { snap ->
            val reason = notificationReasonCode(
                snap.category,
                snap.importance,
                snap.hasFullScreenIntent,
                snap.hasTurnScreenOn,
            )
            WakeCandidate(
                packageName = snap.packageName,
                appLabel = resolveLabel(snap.packageName),
                channelId = snap.channelId,
                channelName = snap.channelName,
                reasonCode = reason,
                confidence = confidenceForActiveNotification(reason),
            )
        }
}

fun mergeNotificationCandidates(
    cached: List<WakeCandidate>,
    active: List<WakeCandidate>,
): List<WakeCandidate> = (cached + active)
    .groupBy { it.packageName to it.channelId }
    .values
    .map { group -> group.maxBy { it.confidence } }
    .sortedByDescending { it.confidence }

private fun confidenceForActiveNotification(reason: ReasonCode): Float = when (reason) {
    ReasonCode.NOTIFICATION_FULL_SCREEN -> 0.88f
    ReasonCode.NOTIFICATION_RING -> 0.85f
    ReasonCode.NOTIFICATION_HEADS_UP -> 0.78f
    ReasonCode.NOTIFICATION_UNKNOWN -> 0.65f
    else -> 0.65f
}

fun capUsageCandidateConfidence(
    candidates: List<WakeCandidate>,
    hasNotifications: Boolean,
): List<WakeCandidate> =
    if (hasNotifications) {
        candidates.map { it.copy(confidence = it.confidence.coerceAtMost(0.45f)) }
    } else {
        candidates
    }

fun rootWakeCandidate(
    snapshot: RootSnapshot,
    resolveLabel: (String) -> String?,
): WakeCandidate? {
    val rootPackage = snapshot.packageName
        ?: PackageFromWakelockTag.extractPackage(snapshot.wakelockTag)
        ?: return null
    return WakeCandidate(
        packageName = rootPackage,
        appLabel = resolveLabel(rootPackage),
        channelId = null,
        channelName = null,
        reasonCode = snapshot.reasonCode ?: ReasonCode.ROOT_WAKELOCK,
        confidence = if (snapshot.packageName != null) 0.85f else 0.55f,
        detail = snapshot.wakelockTag,
    )
}
