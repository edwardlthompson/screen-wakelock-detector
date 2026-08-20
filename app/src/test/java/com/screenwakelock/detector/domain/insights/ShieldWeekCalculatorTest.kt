package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.wakeshield.ShieldOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldWeekCalculatorTest {

    private val now = 1_800_000_000_000L

    private fun event(id: Long, outcome: ShieldOutcome, ageMs: Long = 0): WakeEvent =
        WakeEvent(
            id = id,
            timestampMillis = now - ageMs,
            attributedPackage = "com.app.a",
            attributedAppLabel = "A",
            channelId = "c",
            channelName = "C",
            reasonCode = ReasonCode.NOTIFICATION_HEADS_UP,
            confidence = 0.9f,
            shieldOutcome = outcome.name,
        )

    @Test
    fun summarize_groupsShieldedAllowedOther() {
        val counts = ShieldWeekCalculator.summarize(
            listOf(
                event(1, ShieldOutcome.LOCKED),
                event(2, ShieldOutcome.SLEPT),
                event(3, ShieldOutcome.ALLOWED_EXEMPT),
                event(4, ShieldOutcome.PANIC_DISABLED),
                event(5, ShieldOutcome.NONE),
            ),
            nowMillis = now,
        )
        assertEquals(2, counts.shielded)
        assertEquals(1, counts.allowed)
        assertEquals(1, counts.other)
        assertTrue(counts.hasData)
    }

    @Test
    fun summarize_ignoresOlderThanOneWeek() {
        val week = 7L * 24 * 60 * 60 * 1000
        val counts = ShieldWeekCalculator.summarize(
            listOf(event(1, ShieldOutcome.LOCKED, ageMs = week + 1)),
            nowMillis = now,
        )
        assertFalse(counts.hasData)
    }

    @Test
    fun shieldListFilter_matchesUnknownShieldedAllowed() {
        val unknown = WakeEvent(
            timestampMillis = now,
            attributedPackage = null,
            attributedAppLabel = null,
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.UNKNOWN,
            confidence = 0.1f,
        )
        val shielded = event(2, ShieldOutcome.LOCKED)
        val allowed = event(3, ShieldOutcome.ALLOWED_EXEMPT)
        assertTrue(ShieldListFilter.UNKNOWN.matches(unknown))
        assertTrue(ShieldListFilter.SHIELDED.matches(shielded))
        assertTrue(ShieldListFilter.ALLOWED.matches(allowed))
        assertFalse(ShieldListFilter.SHIELDED.matches(allowed))
    }
}
