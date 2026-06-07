package com.screenwakelock.detector.util

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportUtilsTest {

    private fun sampleEvent(id: Long = 1): WakeEvent = WakeEvent(
        id = id,
        timestampMillis = 1_700_000_000_000L,
        attributedPackage = "com.example.app",
        attributedAppLabel = "Example",
        channelId = "alerts",
        channelName = "Alerts",
        reasonCode = ReasonCode.NOTIFICATION_HEADS_UP,
        confidence = 0.85f,
        screenOffDurationMs = 3500L,
    )

    @Test
    fun toJson_containsExpectedFields() {
        val json = ExportUtils.toJson(listOf(sampleEvent()))
        assertTrue(json.contains("\"id\":1"))
        assertTrue(json.contains("\"attributedPackage\":\"com.example.app\""))
        assertTrue(json.contains("\"reasonCode\":\"NOTIFICATION_HEADS_UP\""))
        assertTrue(json.contains("\"screenOffDurationMs\":3500"))
    }

    @Test
    fun toJson_multipleEvents_isArray() {
        val json = ExportUtils.toJson(listOf(sampleEvent(1), sampleEvent(2)))
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"id\":1"))
        assertTrue(json.contains("\"id\":2"))
    }

    @Test
    fun filterByDateRange_respectsBounds() {
        val events = listOf(
            sampleEvent(1).copy(timestampMillis = 100L),
            sampleEvent(2).copy(timestampMillis = 200L),
            sampleEvent(3).copy(timestampMillis = 300L),
        )
        val filtered = ExportUtils.filterByDateRange(events, 150L, 250L)
        assertEquals(1, filtered.size)
        assertEquals(2L, filtered.first().id)
    }

    @Test
    fun toCsv_includesScreenOffDuration() {
        val csv = ExportUtils.toCsv(listOf(sampleEvent()))
        assertTrue(csv.contains("screen_off_duration_ms"))
        assertTrue(csv.contains("3500"))
    }
}
