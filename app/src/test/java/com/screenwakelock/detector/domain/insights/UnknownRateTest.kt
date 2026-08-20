package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnknownRateTest {

    private val now = 1_800_000_000_000L

    private fun event(id: Long, unknown: Boolean, ageMs: Long = 0): WakeEvent =
        WakeEvent(
            id = id,
            timestampMillis = now - ageMs,
            attributedPackage = if (unknown) null else "com.app.a",
            attributedAppLabel = if (unknown) null else "A",
            channelId = null,
            channelName = null,
            reasonCode = if (unknown) ReasonCode.UNKNOWN else ReasonCode.NOTIFICATION_HEADS_UP,
            confidence = if (unknown) 0.2f else 0.9f,
        )

    @Test
    fun compute_countsUnknownInWindow() {
        val snap = UnknownRate.compute(
            listOf(event(1, true), event(2, false), event(3, true)),
            nowMillis = now,
        )
        assertEquals(2, snap.unknown)
        assertEquals(3, snap.total)
        assertTrue(snap.shouldShow())
        assertEquals("2 of 3 wakes unattributed", snap.chipLabel())
    }

    @Test
    fun shouldShow_falseWhenTooFewEvents() {
        val snap = UnknownRate.compute(listOf(event(1, true)), nowMillis = now)
        assertFalse(snap.shouldShow())
    }

    @Test
    fun isUnknown_falseWhenTagDerivesPackage() {
        val event = WakeEvent(
            id = 9,
            timestampMillis = now,
            attributedPackage = null,
            attributedAppLabel = null,
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.UNKNOWN,
            confidence = 0.2f,
            wakelockTag = "com.life360.android.safetymapd:gps",
        )
        assertFalse(UnknownRate.isUnknown(event))
    }
}
