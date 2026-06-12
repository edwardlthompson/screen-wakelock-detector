package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculatorTest {

    private fun event(
        id: Long,
        pkg: String?,
        channelId: String? = "c1",
        hourOffset: Long = 0,
    ): WakeEvent {
        val base = 1_700_000_000_000L
        return WakeEvent(
            id = id,
            timestampMillis = base + hourOffset,
            attributedPackage = pkg,
            attributedAppLabel = pkg?.replaceFirstChar { it.uppercase() },
            channelId = channelId,
            channelName = "Channel",
            reasonCode = ReasonCode.NOTIFICATION_HEADS_UP,
            confidence = 0.9f,
        )
    }

    @Test
    fun compute_totalAndNighttimeCounts() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val dayMillis = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 2)
        val nightMillis = cal.timeInMillis

        val events = listOf(
            event(1, "com.app.a").copy(timestampMillis = dayMillis),
            event(2, "com.app.a").copy(timestampMillis = dayMillis + 3_600_000),
            event(3, "com.app.b").copy(timestampMillis = nightMillis),
        )
        val insights = InsightsCalculator.compute(events, nighttimeStartHour = 23, nighttimeEndHour = 6)
        assertEquals(3, insights.totalWakes)
        assertEquals(1, insights.nighttimeWakes)
    }

    @Test
    fun compute_excludesIgnoredPackages() {
        val events = listOf(
            event(1, "com.app.a"),
            event(2, "com.app.b"),
            event(3, "com.app.a"),
        )
        val insights = InsightsCalculator.compute(
            events,
            ignoredPackages = setOf("com.app.a"),
        )
        assertEquals(1, insights.totalWakes)
        assertEquals(1, insights.topOffenders.size)
        assertEquals("com.app.b", insights.topOffenders.first().packageName)
    }

    @Test
    fun compute_excludesTagOnlyIgnoredPackages() {
        val events = listOf(
            event(1, "com.app.a"),
            event(2, null).copy(
                wakelockTag = "com.app.a:alarm",
                reasonCode = ReasonCode.ROOT_WAKELOCK,
                confidence = 0.55f,
            ),
            event(3, "com.app.b"),
        )
        val insights = InsightsCalculator.compute(
            events,
            ignoredPackages = setOf("com.app.a"),
        )
        assertEquals(1, insights.totalWakes)
        assertEquals("com.app.b", insights.topOffenders.first().packageName)
    }

    @Test
    fun compute_topOffenders_sortedByCount() {
        val events = listOf(
            event(1, "com.app.a"),
            event(2, "com.app.a"),
            event(3, "com.app.b"),
        )
        val insights = InsightsCalculator.compute(events)
        assertEquals(2, insights.topOffenders.size)
        assertEquals("com.app.a", insights.topOffenders.first().packageName)
        assertEquals(2, insights.topOffenders.first().count)
    }

    @Test
    fun buildHeatmap_aggregatesByDayAndHour() {
        val events = listOf(
            event(1, "com.app.a", hourOffset = 0),
            event(2, "com.app.a", hourOffset = 0),
        )
        val heatmap = InsightsCalculator.buildHeatmap(events)
        val total = heatmap.sumOf { it.count }
        assertEquals(2, total)
    }

    @Test
    fun buildHeatmap_matchesHistoryFilterByDayAndHour() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 14)
        cal.set(java.util.Calendar.MINUTE, 30)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val ts = cal.timeInMillis

        val events = listOf(
            event(1, "com.app.a").copy(timestampMillis = ts),
            event(2, "com.app.b").copy(timestampMillis = ts),
            event(3, "com.app.c").copy(timestampMillis = ts + 3_600_000),
        )
        val heatmap = InsightsCalculator.buildHeatmap(events)
        val day = com.screenwakelock.detector.util.TimeUtils.dayOfWeek(ts)
        val hour = com.screenwakelock.detector.util.TimeUtils.hourOfDay(ts)
        val cellCount = heatmap.first { it.dayOfWeek == day && it.hourOfDay == hour }.count
        val historyCount = events.count {
            com.screenwakelock.detector.util.TimeUtils.dayOfWeek(it.timestampMillis) == day &&
                com.screenwakelock.detector.util.TimeUtils.hourOfDay(it.timestampMillis) == hour
        }
        assertEquals(historyCount, cellCount)
    }

    @Test
    fun detectRecurringPatterns_requiresThreeNights() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 2)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val events = (0 until 3).flatMap { nightOffset ->
            val nightCal = cal.clone() as java.util.Calendar
            nightCal.add(java.util.Calendar.DAY_OF_YEAR, -nightOffset)
            listOf(
                event(nightOffset.toLong() + 1, "com.night.app", channelId = "alerts")
                    .copy(timestampMillis = nightCal.timeInMillis),
            )
        }
        val patterns = InsightsCalculator.detectRecurringPatterns(events, minNights = 3)
        assertEquals(1, patterns.size)
        assertEquals("com.night.app", patterns.first().packageName)
        assertEquals(3, patterns.first().consecutiveNights)
    }

    @Test
    fun computeWeekOverWeek_comparesSevenDayWindows() {
        val now = 1_700_500_000_000L
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val events = listOf(
            event(1, "com.app.a").copy(timestampMillis = now - 1_000),
            event(2, "com.app.a").copy(timestampMillis = now - weekMs + 1_000),
            event(3, "com.app.a").copy(timestampMillis = now - weekMs - 1_000),
            event(4, "com.app.a").copy(timestampMillis = now - weekMs * 2 + 1_000),
        )
        val wow = InsightsCalculator.computeWeekOverWeek(events, nowMillis = now)
        assertEquals(2, wow.current)
        assertEquals(2, wow.previous)
        assertEquals(0f, wow.deltaPercent)
    }
}
