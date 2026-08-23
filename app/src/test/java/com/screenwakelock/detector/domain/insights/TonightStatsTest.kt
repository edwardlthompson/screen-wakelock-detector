package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.wakeshield.ShieldOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonightStatsTest {

    @Test
    fun countsTonightAndEnforcements() {
        val now = 1_700_000_000_000L
        val events = listOf(
            event(now - 1_000, "com.a", ShieldOutcome.LOCKED.name),
            event(now - 2_000, "com.a", ShieldOutcome.WOULD_HAVE_BLOCKED.name),
            event(now - 40L * 24 * 60 * 60 * 1000, "com.old", null),
        )
        val snap = TonightStats.compute(events, nowMillis = now)
        assertTrue(snap.count >= 2)
        assertEquals("com.a", snap.topPackage)
        assertEquals(2, snap.enforcementCount)
        assertEquals(7, snap.dailyCounts.size)
        assertTrue(TonightStats.monthCount(events, now) >= 2)
    }

    private fun event(ts: Long, pkg: String, outcome: String?) = WakeEvent(
        timestampMillis = ts,
        attributedPackage = pkg,
        attributedAppLabel = pkg,
        channelId = null,
        channelName = null,
        reasonCode = ReasonCode.NOTIFICATION_HEADS_UP,
        confidence = 0.8f,
        shieldOutcome = outcome,
    )
}
