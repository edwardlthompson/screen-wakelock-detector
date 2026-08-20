package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.wakeshield.ShieldOutcome

data class ShieldWeekCounts(
    val shielded: Int,
    val allowed: Int,
    val other: Int,
) {
    val hasData: Boolean get() = shielded + allowed + other > 0
}

object ShieldWeekCalculator {
    private val shieldedOutcomes = setOf(
        ShieldOutcome.CANCELLED_NOTIFS,
        ShieldOutcome.LOCKED,
        ShieldOutcome.SLEPT,
        ShieldOutcome.DENIED_APPOP,
        ShieldOutcome.PARTIAL,
    )
    private val allowedOutcomes = setOf(
        ShieldOutcome.ALLOWED_EXEMPT,
        ShieldOutcome.ALLOWED_FSI,
        ShieldOutcome.ABORTED_INTERACTIVE,
    )

    fun summarize(
        events: List<WakeEvent>,
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = 7L * 24 * 60 * 60 * 1000,
    ): ShieldWeekCounts {
        val start = nowMillis - windowMs
        var shielded = 0
        var allowed = 0
        var other = 0
        events.forEach { event ->
            if (event.timestampMillis < start) return@forEach
            val outcome = ShieldOutcome.fromStorage(event.shieldOutcome)
            if (outcome == ShieldOutcome.NONE) return@forEach
            when (outcome) {
                in shieldedOutcomes -> shielded++
                in allowedOutcomes -> allowed++
                else -> other++
            }
        }
        return ShieldWeekCounts(shielded, allowed, other)
    }

    fun isShielded(event: WakeEvent): Boolean =
        ShieldOutcome.fromStorage(event.shieldOutcome) in shieldedOutcomes

    fun isAllowed(event: WakeEvent): Boolean =
        ShieldOutcome.fromStorage(event.shieldOutcome) in allowedOutcomes
}

enum class ShieldListFilter(val label: String) {
    UNKNOWN("Unknown"),
    SHIELDED("Shielded"),
    ALLOWED("Allowed"),
    ;

    fun matches(event: WakeEvent): Boolean = when (this) {
        UNKNOWN -> UnknownRate.isUnknown(event)
        SHIELDED -> ShieldWeekCalculator.isShielded(event)
        ALLOWED -> ShieldWeekCalculator.isAllowed(event)
    }
}
