package com.screenwakelock.detector.domain.model

data class OffenderSummary(
    val packageName: String,
    val appLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val count: Int,
    val lastTimestampMillis: Long,
    val nighttimeCount: Int,
)

data class RecurringPattern(
    val packageName: String,
    val appLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val nightCount: Int,
    val consecutiveNights: Int,
)

data class HeatmapCell(
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val count: Int,
)

data class InsightsData(
    val totalWakes: Int,
    val nighttimeWakes: Int,
    val weekOverWeekCurrent: Int,
    val weekOverWeekPrevious: Int,
    val weekOverWeekDeltaPercent: Float?,
    val topOffenders: List<OffenderSummary>,
    val recurringPatterns: List<RecurringPattern>,
    val heatmap: List<HeatmapCell>,
)
