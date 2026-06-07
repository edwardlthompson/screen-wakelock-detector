package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.HeatmapCell
import com.screenwakelock.detector.domain.model.InsightsData
import com.screenwakelock.detector.domain.model.OffenderSummary
import com.screenwakelock.detector.domain.model.RecurringPattern
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.TimeUtils

object InsightsCalculator {
    fun compute(
        events: List<WakeEvent>,
        nighttimeStartHour: Int = 23,
        nighttimeEndHour: Int = 6,
    ): InsightsData {
        val nighttime = events.filter {
            TimeUtils.isNighttime(it.timestampMillis, nighttimeStartHour, nighttimeEndHour)
        }

        val offenders = events
            .filter { it.attributedPackage != null }
            .groupBy { "${it.attributedPackage}:${it.channelId ?: ""}" }
            .map { (_, group) ->
                val latest = group.maxBy { it.timestampMillis }
                OffenderSummary(
                    packageName = latest.attributedPackage!!,
                    appLabel = latest.attributedAppLabel,
                    channelId = latest.channelId,
                    channelName = latest.channelName,
                    count = group.size,
                    lastTimestampMillis = latest.timestampMillis,
                    nighttimeCount = group.count {
                        TimeUtils.isNighttime(it.timestampMillis, nighttimeStartHour, nighttimeEndHour)
                    },
                )
            }
            .sortedByDescending { it.count }
            .take(10)

        val patterns = detectRecurringPatterns(events, nighttimeStartHour, nighttimeEndHour)

        val heatmap = buildHeatmap(events)

        return InsightsData(
            totalWakes = events.size,
            nighttimeWakes = nighttime.size,
            topOffenders = offenders,
            recurringPatterns = patterns,
            heatmap = heatmap,
        )
    }

    fun detectRecurringPatterns(
        events: List<WakeEvent>,
        nighttimeStartHour: Int = 23,
        nighttimeEndHour: Int = 6,
        minNights: Int = 3,
    ): List<RecurringPattern> {
        val nightEvents = events.filter {
            it.attributedPackage != null &&
                TimeUtils.isNighttime(it.timestampMillis, nighttimeStartHour, nighttimeEndHour)
        }
        return nightEvents
            .groupBy { "${it.attributedPackage}:${it.channelId ?: ""}" }
            .mapNotNull { (_, group) ->
                val nights = group.map { TimeUtils.nightKey(it.timestampMillis) }.distinct()
                if (nights.size < minNights) return@mapNotNull null
                val latest = group.maxBy { it.timestampMillis }
                RecurringPattern(
                    packageName = latest.attributedPackage!!,
                    appLabel = latest.attributedAppLabel,
                    channelId = latest.channelId,
                    channelName = latest.channelName,
                    nightCount = group.size,
                    consecutiveNights = nights.size,
                )
            }
            .sortedByDescending { it.consecutiveNights }
    }

    fun buildHeatmap(events: List<WakeEvent>): List<HeatmapCell> {
        val counts = mutableMapOf<Pair<Int, Int>, Int>()
        events.forEach { event ->
            val key = TimeUtils.dayOfWeek(event.timestampMillis) to
                TimeUtils.hourOfDay(event.timestampMillis)
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts.map { (key, count) ->
            HeatmapCell(dayOfWeek = key.first, hourOfDay = key.second, count = count)
        }
    }
}
