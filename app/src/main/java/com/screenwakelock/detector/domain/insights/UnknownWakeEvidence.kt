package com.screenwakelock.detector.domain.insights

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.domain.model.WakeEventIdentity

data class UnknownWakeEvidence(
    val lines: List<String>,
    val grantHint: String?,
) {
    fun joined(): String = lines.joinToString(" ")
}

object UnknownWakeEvidenceBuilder {
    fun forEvent(
        event: WakeEvent,
        listenerGranted: Boolean,
        usageGranted: Boolean,
        rootEnabled: Boolean,
        lastDumpsysOk: Boolean,
    ): UnknownWakeEvidence {
        val unknown = UnknownRate.isUnknown(event)
        val lines = mutableListOf<String>()
        if (unknown) {
            if (!listenerGranted) {
                lines += "Notification access was off, so the channel could not be matched."
            }
            if (!usageGranted) {
                lines += "Usage access was off, so the foreground-app fallback was skipped."
            }
            if (!rootEnabled) {
                lines += "Root attribution is off, so wakelock holders were not read."
            } else if (!lastDumpsysOk) {
                lines += "Root is on, but the last dumpsys parse did not return a holder."
            }
            if (lines.isEmpty()) {
                lines += "No notification, usage, or wakelock signal landed in the correlation window."
            }
        } else {
            lines += event.reasonCode.explainer()
            if (event.reasonCode == ReasonCode.USAGE_STATS_FOREGROUND ||
                event.reasonCode == ReasonCode.USAGE_STATS_RECENT
            ) {
                lines += "This is a usage-stats fallback — the app was nearby, not a proven channel match."
            }
        }
        val grantHint = when {
            unknown && !listenerGranted -> "notification_access"
            unknown && !usageGranted -> "usage_access"
            else -> null
        }
        return UnknownWakeEvidence(lines, grantHint)
    }

    fun grantImpactHint(
        snapshot: UnknownRateSnapshot,
        listenerGranted: Boolean,
        usageGranted: Boolean,
    ): String? {
        if (snapshot.total < 3) return null
        return when {
            !listenerGranted ->
                "Granting notification access usually cuts unknown wakes — ${snapshot.chipLabel()}."
            !usageGranted ->
                "Granting usage access adds a foreground fallback — ${snapshot.chipLabel()}."
            else -> null
        }
    }

    fun isLowImportanceHint(event: WakeEvent): Boolean {
        val detail = event.shieldDetail.orEmpty() + event.channelName.orEmpty()
        return event.reasonCode == ReasonCode.NOTIFICATION_UNKNOWN &&
            (detail.contains("low", ignoreCase = true) ||
                detail.contains("min", ignoreCase = true) ||
                detail.contains("silent", ignoreCase = true) ||
                event.channelId?.contains("low", ignoreCase = true) == true)
    }

    fun effectiveUnknown(event: WakeEvent): Boolean =
        WakeEventIdentity.effectivePackage(event) == null && UnknownRate.isUnknown(event)
}
