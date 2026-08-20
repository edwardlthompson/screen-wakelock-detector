package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent

data class UnknownRateSnapshot(
    val unknown: Int,
    val total: Int,
) {
    val ratio: Float get() = if (total == 0) 0f else unknown.toFloat() / total

    fun shouldShow(): Boolean = total >= 3 && (unknown >= 2 || ratio >= 0.25f)

    fun chipLabel(): String = "$unknown of $total wakes unattributed"
}

object UnknownRate {
    fun isUnknown(event: WakeEvent): Boolean {
        if (com.screenwakelock.detector.domain.model.WakeEventIdentity.effectivePackage(event) != null) {
            return false
        }
        return event.attributedPackage == null || event.reasonCode == ReasonCode.UNKNOWN
    }

    fun compute(
        events: List<WakeEvent>,
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = 7L * 24 * 60 * 60 * 1000,
    ): UnknownRateSnapshot {
        val start = nowMillis - windowMs
        val window = events.filter { it.timestampMillis >= start }
        return UnknownRateSnapshot(
            unknown = window.count { isUnknown(it) },
            total = window.size,
        )
    }
}
