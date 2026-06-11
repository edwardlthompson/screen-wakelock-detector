package com.screenwakelock.detector.util

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeEventFiltersTest {

    @Test
    fun isVisibleInLists_allowsNullPackage() {
        val event = WakeEvent(
            timestampMillis = 1L,
            attributedPackage = null,
            attributedAppLabel = null,
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.UNKNOWN,
            confidence = 0f,
        )
        assertTrue(WakeEventFilters.isVisibleInLists(event, setOf("com.other.app")))
    }

    @Test
    fun isVisibleInLists_hidesIgnoredPackage() {
        val event = WakeEvent(
            timestampMillis = 1L,
            attributedPackage = "com.ignored.app",
            attributedAppLabel = "Ignored",
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
            confidence = 0.8f,
        )
        assertFalse(
            WakeEventFilters.isVisibleInLists(event, setOf("com.ignored.app")),
        )
        assertTrue(
            WakeEventFilters.isVisibleInLists(event, emptySet()),
        )
    }

    @Test
    fun filterVisible_excludesIgnoredOnly() {
        val visible = WakeEvent(
            timestampMillis = 1L,
            attributedPackage = "com.visible.app",
            attributedAppLabel = "Visible",
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
            confidence = 0.8f,
        )
        val hidden = visible.copy(
            id = 2L,
            attributedPackage = "com.hidden.app",
            attributedAppLabel = "Hidden",
        )
        val result = WakeEventFilters.filterVisible(
            listOf(visible, hidden),
            setOf("com.hidden.app"),
        )
        assertEquals(1, result.size)
        assertEquals("com.visible.app", result.first().attributedPackage)
    }
}
