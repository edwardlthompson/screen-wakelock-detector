package com.screenwakelock.detector.domain.attributor

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeCandidate
import com.screenwakelock.detector.root.RootSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeAttributorLogicTest {

    @Test
    fun capUsageCandidateConfidence_capsWhenNotificationsPresent() {
        val candidates = listOf(
            WakeCandidate(
                packageName = "com.example.app",
                appLabel = "Example",
                channelId = null,
                channelName = null,
                reasonCode = ReasonCode.USAGE_STATS_FOREGROUND,
                confidence = 0.7f,
            ),
        )
        val capped = capUsageCandidateConfidence(candidates, hasNotifications = true)
        assertEquals(0.45f, capped.first().confidence)
    }

    @Test
    fun capUsageCandidateConfidence_unchangedWhenNoNotifications() {
        val candidates = listOf(
            WakeCandidate(
                packageName = "com.example.app",
                appLabel = "Example",
                channelId = null,
                channelName = null,
                reasonCode = ReasonCode.USAGE_STATS_FOREGROUND,
                confidence = 0.7f,
            ),
        )
        val result = capUsageCandidateConfidence(candidates, hasNotifications = false)
        assertEquals(0.7f, result.first().confidence)
    }

    @Test
    fun rootWakeCandidate_tagOnlySnapshot() {
        val snapshot = RootSnapshot(
            wakelockTag = "com.example.app:notification",
            wakelockName = "notification",
            uid = null,
            reasonCode = ReasonCode.ROOT_WAKELOCK,
            packageName = null,
            parserId = "test",
        )
        val candidate = rootWakeCandidate(snapshot) { "Example" }
        assertTrue(candidate != null)
        assertEquals("com.example.app", candidate!!.packageName)
        assertEquals(0.55f, candidate.confidence)
        assertEquals("Example", candidate.appLabel)
    }

    @Test
    fun rootWakeCandidate_uidPackageHigherConfidence() {
        val snapshot = RootSnapshot(
            wakelockTag = "com.example.app:notification",
            wakelockName = "notification",
            uid = 10_000,
            reasonCode = ReasonCode.ROOT_WAKELOCK,
            packageName = "com.example.app",
            parserId = "test",
        )
        val candidate = rootWakeCandidate(snapshot) { null }
        assertEquals(0.85f, candidate!!.confidence)
    }

    @Test
    fun rootWakeCandidate_nullWhenNoPackageIdentity() {
        val snapshot = RootSnapshot(
            wakelockTag = "wlan",
            wakelockName = "wlan",
            uid = null,
            reasonCode = ReasonCode.ROOT_WAKEUP_SOURCE,
            packageName = null,
            parserId = "test",
        )
        assertNull(rootWakeCandidate(snapshot) { null })
    }

    @Test
    fun notificationReasonCode_fullScreenIntent() {
        assertEquals(
            ReasonCode.NOTIFICATION_FULL_SCREEN,
            notificationReasonCode(
                category = null,
                importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
                hasFullScreenIntent = true,
            ),
        )
    }

    @Test
    fun activeNotificationCandidates_includesDefaultImportance_skipsLow() {
        val snapshots = listOf(
            com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot(
                packageName = "com.example.low",
                channelId = "misc",
                channelName = "Misc",
                category = null,
                importance = android.app.NotificationManager.IMPORTANCE_LOW,
                hasFullScreenIntent = false,
                hasTurnScreenOn = false,
            ),
            com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot(
                packageName = "com.example.default",
                channelId = "shell_cmd",
                channelName = "Shell",
                category = null,
                importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
                hasFullScreenIntent = false,
                hasTurnScreenOn = false,
            ),
            com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot(
                packageName = "com.example.alarm",
                channelId = "alarms",
                channelName = "Alarms",
                category = android.app.Notification.CATEGORY_ALARM,
                importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
                hasFullScreenIntent = false,
                hasTurnScreenOn = false,
            ),
        )
        val result = activeNotificationCandidates(snapshots, emptyList()) { "Label" }
        assertEquals(2, result.size)
        assertEquals("com.example.default", result[0].packageName)
        assertEquals(0.58f, result[0].confidence)
        assertEquals("com.example.alarm", result[1].packageName)
        assertEquals(ReasonCode.NOTIFICATION_FULL_SCREEN, result[1].reasonCode)
    }

    @Test
    fun activeNotificationCandidates_doesNotDedupAgainstCache_mergeKeepsFsi() {
        val cached = listOf(
            WakeCandidate(
                packageName = "com.example.app",
                appLabel = "Example",
                channelId = "alerts",
                channelName = "Alerts",
                reasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
                confidence = 0.55f,
            ),
        )
        val snapshots = listOf(
            com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot(
                packageName = "com.example.app",
                channelId = "alerts",
                channelName = "Alerts",
                category = null,
                importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
                hasFullScreenIntent = true,
                hasTurnScreenOn = false,
            ),
        )
        val active = activeNotificationCandidates(snapshots, cached) { "Example" }
        assertEquals(1, active.size)
        val merged = mergeNotificationCandidates(cached, active)
        assertEquals(1, merged.size)
        assertEquals(ReasonCode.NOTIFICATION_FULL_SCREEN, merged.first().reasonCode)
        assertEquals(0.88f, merged.first().confidence)
    }

    @Test
    fun mergeNotificationCandidates_keepsHigherConfidence() {
        val cached = listOf(
            WakeCandidate(
                packageName = "com.example.app",
                appLabel = "Example",
                channelId = "alerts",
                channelName = "Alerts",
                reasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
                confidence = 0.55f,
            ),
        )
        val active = listOf(
            WakeCandidate(
                packageName = "com.example.app",
                appLabel = "Example",
                channelId = "alerts",
                channelName = "Alerts",
                reasonCode = ReasonCode.NOTIFICATION_FULL_SCREEN,
                confidence = 0.88f,
            ),
        )
        val merged = mergeNotificationCandidates(cached, active)
        assertEquals(1, merged.size)
        assertEquals(ReasonCode.NOTIFICATION_FULL_SCREEN, merged.first().reasonCode)
        assertEquals(0.88f, merged.first().confidence)
    }

    @Test
    fun cachedNotificationCandidates_usesFullScreenFlags() {
        val cached = listOf(
            com.screenwakelock.detector.domain.model.CachedNotification(
                packageName = "com.example.fsi",
                channelId = "alerts",
                channelName = "Alerts",
                postedAtMillis = 1_000L,
                category = null,
                importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
                hasFullScreenIntent = true,
                hasTurnScreenOn = false,
            ),
        )
        val result = cachedNotificationCandidates(
            cached,
            screenOnMillis = 1_000L,
            correlationWindowMs = 5_000L,
        ) { "FSI" }
        assertEquals(ReasonCode.NOTIFICATION_FULL_SCREEN, result.first().reasonCode)
    }
}
