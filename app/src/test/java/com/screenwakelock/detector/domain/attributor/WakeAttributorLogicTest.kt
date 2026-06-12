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
}
