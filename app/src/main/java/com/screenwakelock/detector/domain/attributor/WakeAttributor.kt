package com.screenwakelock.detector.domain.attributor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import com.screenwakelock.detector.domain.model.AttributionData
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeCandidate
import com.screenwakelock.detector.root.RootAttributor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeAttributor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootAttributor: RootAttributor,
) {
    suspend fun attribute(
        screenOnMillis: Long,
        notificationCache: NotificationCacheRepository,
        rootEnabled: Boolean,
        correlationWindowMs: Long = DEFAULT_WINDOW_MS,
    ): AttributionData {
        val start = screenOnMillis - correlationWindowMs
        val end = screenOnMillis + correlationWindowMs

        val notifications = notificationCache.getInWindow(start, end)
        val notificationCandidates = notifications.map { notif ->
            val reason = when {
                notif.importance >= android.app.NotificationManager.IMPORTANCE_HIGH ->
                    ReasonCode.NOTIFICATION_HEADS_UP
                notif.category == android.app.Notification.CATEGORY_CALL ->
                    ReasonCode.NOTIFICATION_RING
                else -> ReasonCode.NOTIFICATION_UNKNOWN
            }
            val proximity = 1f - (
                kotlin.math.abs(notif.postedAtMillis - screenOnMillis).toFloat() /
                    correlationWindowMs
                ).coerceIn(0f, 1f)
            WakeCandidate(
                packageName = notif.packageName,
                appLabel = resolveAppLabel(notif.packageName),
                channelId = notif.channelId,
                channelName = notif.channelName,
                reasonCode = reason,
                confidence = 0.5f + proximity * 0.45f,
            )
        }.sortedByDescending { it.confidence }

        var rootSnapshot = if (rootEnabled) {
            rootAttributor.captureSnapshot { uid -> uidToPackage(uid) }
        } else {
            null
        }

        val usageCandidates = if (notificationCandidates.isEmpty()) {
            findUsageCandidates(screenOnMillis)
        } else {
            emptyList()
        }

        val allCandidates = buildList {
            addAll(notificationCandidates)
            addAll(usageCandidates)
            rootSnapshot?.let { snap ->
                if (snap.packageName != null) {
                    add(
                        WakeCandidate(
                            packageName = snap.packageName,
                            appLabel = resolveAppLabel(snap.packageName),
                            channelId = null,
                            channelName = null,
                            reasonCode = snap.reasonCode ?: ReasonCode.ROOT_WAKELOCK,
                            confidence = 0.85f,
                            detail = snap.wakelockTag,
                        ),
                    )
                }
            }
        }.sortedByDescending { it.confidence }

        val top = allCandidates.firstOrNull()
        val reasonCode = when {
            allCandidates.size > 1 && (top?.confidence ?: 0f) < 0.75f ->
                ReasonCode.MULTIPLE_CANDIDATES
            top != null -> top.reasonCode
            else -> ReasonCode.UNKNOWN
        }

        return AttributionData(
            packageName = top?.packageName,
            appLabel = top?.appLabel,
            channelId = top?.channelId,
            channelName = top?.channelName,
            reasonCode = reasonCode,
            confidence = top?.confidence ?: 0f,
            candidates = allCandidates.take(5),
            rootEnhanced = rootSnapshot != null,
            wakelockTag = rootSnapshot?.wakelockTag,
            wakelockName = rootSnapshot?.wakelockName,
        )
    }

    private fun findUsageCandidates(screenOnMillis: Long): List<WakeCandidate> {
        if (!hasUsageStatsPermission()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = screenOnMillis - DEFAULT_WINDOW_MS * 2
        val events = usm.queryEvents(start, screenOnMillis + DEFAULT_WINDOW_MS)
        val event = UsageEvents.Event()
        var lastPackage: String? = null
        var lastTime = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                if (event.timeStamp >= lastTime) {
                    lastPackage = event.packageName
                    lastTime = event.timeStamp
                }
            }
        }
        if (lastPackage != null) {
            val proximity = 1f - (
                kotlin.math.abs(lastTime - screenOnMillis).toFloat() / DEFAULT_WINDOW_MS
                ).coerceIn(0f, 1f)
            return listOf(
                WakeCandidate(
                    packageName = lastPackage,
                    appLabel = resolveAppLabel(lastPackage),
                    channelId = null,
                    channelName = null,
                    reasonCode = ReasonCode.USAGE_STATS_FOREGROUND,
                    confidence = 0.35f + proximity * 0.35f,
                ),
            )
        }

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            screenOnMillis,
        )
        val recent = stats?.maxByOrNull { it.lastTimeUsed }
        if (recent != null && screenOnMillis - recent.lastTimeUsed < DEFAULT_WINDOW_MS * 3) {
            return listOf(
                WakeCandidate(
                    packageName = recent.packageName,
                    appLabel = resolveAppLabel(recent.packageName),
                    channelId = null,
                    channelName = null,
                    reasonCode = ReasonCode.USAGE_STATS_RECENT,
                    confidence = 0.3f,
                ),
            )
        }
        return emptyList()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun resolveAppLabel(packageName: String): String? = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(info).toString()
    }.getOrNull()

    private fun uidToPackage(uid: Int): String? {
        val pm = context.packageManager
        return pm.getPackagesForUid(uid)?.firstOrNull()
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 5_000L
    }
}
