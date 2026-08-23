package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.TimeUtils
import com.screenwakelock.detector.wakeshield.ShieldOutcome

data class TonightSnapshot(
    val count: Int,
    val topPackage: String?,
    val topLabel: String?,
    val enforcementCount: Int,
    val lastEnforcements: List<WakeEvent>,
    val dailyCounts: List<Int>,
) {
    val overBudget: Boolean get() = count >= 8
}

object TonightStats {
    private val enforced = setOf(
        ShieldOutcome.CANCELLED_NOTIFS,
        ShieldOutcome.LOCKED,
        ShieldOutcome.SLEPT,
        ShieldOutcome.DENIED_APPOP,
        ShieldOutcome.PARTIAL,
        ShieldOutcome.WOULD_HAVE_BLOCKED,
    )

    fun compute(
        events: List<WakeEvent>,
        nowMillis: Long = System.currentTimeMillis(),
        nightStartHour: Int = 23,
        nightEndHour: Int = 6,
    ): TonightSnapshot {
        val key = TimeUtils.nightKey(nowMillis)
        val tonight = events.filter { TimeUtils.nightKey(it.timestampMillis) == key }
        val top = tonight
            .filter { !it.attributedPackage.isNullOrBlank() }
            .groupBy { it.attributedPackage }
            .maxByOrNull { it.value.size }
            ?.value
            ?.maxByOrNull { it.timestampMillis }
        val enforcements = tonight.filter {
            ShieldOutcome.fromStorage(it.shieldOutcome) in enforced
        }.sortedByDescending { it.timestampMillis }
        val dayMs = 24L * 60 * 60 * 1000
        val daily = (6 downTo 0).map { offset ->
            val start = nowMillis - offset * dayMs
            val end = start + dayMs
            events.count { it.timestampMillis in start until end }
        }
        return TonightSnapshot(
            count = tonight.size,
            topPackage = top?.attributedPackage,
            topLabel = top?.attributedAppLabel,
            enforcementCount = enforcements.size,
            lastEnforcements = enforcements.take(5),
            dailyCounts = daily,
        )
    }

    fun isEnforced(event: WakeEvent): Boolean =
        ShieldOutcome.fromStorage(event.shieldOutcome) in enforced

    fun monthCount(events: List<WakeEvent>, nowMillis: Long = System.currentTimeMillis()): Int {
        val start = nowMillis - 30L * 24 * 60 * 60 * 1000
        return events.count { it.timestampMillis >= start }
    }

    fun sameWakeCount(event: WakeEvent, events: List<WakeEvent>, nowMillis: Long): Int {
        val start = nowMillis - 7L * 24 * 60 * 60 * 1000
        val pkg = event.attributedPackage
        return events.count {
            it.timestampMillis >= start &&
                it.attributedPackage == pkg &&
                it.channelId == event.channelId
        }
    }

    fun stillWakeAtNight(
        events: List<WakeEvent>,
        ignored: Set<String>,
        nightIgnored: Set<String>,
        startHour: Int,
        endHour: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<String> {
        val start = nowMillis - 7L * 24 * 60 * 60 * 1000
        return events
            .filter {
                it.timestampMillis >= start &&
                    TimeUtils.isNighttime(it.timestampMillis, startHour, endHour) &&
                    !it.attributedPackage.isNullOrBlank() &&
                    it.attributedPackage !in ignored &&
                    it.attributedPackage !in nightIgnored
            }
            .mapNotNull { it.attributedPackage }
            .distinct()
            .take(8)
    }
}
