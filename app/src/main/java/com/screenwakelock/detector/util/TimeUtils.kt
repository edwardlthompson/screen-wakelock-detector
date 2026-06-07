package com.screenwakelock.detector.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    fun formatTime(timestampMillis: Long): String =
        timeFormat.format(Date(timestampMillis))

    fun formatDateTime(timestampMillis: Long): String =
        dateTimeFormat.format(Date(timestampMillis))

    fun formatRelative(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = nowMillis - timestampMillis
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hr ago"
            }
            else -> formatDateTime(timestampMillis)
        }
    }

    fun isNighttime(
        timestampMillis: Long,
        startHour: Int = 23,
        endHour: Int = 6,
    ): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }
    }

    fun dayOfWeek(timestampMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun hourOfDay(timestampMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun nightKey(timestampMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour < 6) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
