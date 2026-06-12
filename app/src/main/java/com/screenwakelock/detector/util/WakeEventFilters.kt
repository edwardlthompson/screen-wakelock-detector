package com.screenwakelock.detector.util

import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.domain.model.WakeEventIdentity

object WakeEventFilters {
    fun isVisibleInLists(event: WakeEvent, ignoredPackages: Set<String>): Boolean =
        !WakeEventIdentity.isIgnored(event, ignoredPackages)

    fun filterVisible(events: List<WakeEvent>, ignoredPackages: Set<String>): List<WakeEvent> =
        events.filter { isVisibleInLists(it, ignoredPackages) }

    /** History search predicate — resolver supplies display names at read time. */
    fun matchesHistoryQuery(
        event: WakeEvent,
        query: String,
        resolveAppName: (WakeEvent) -> String,
    ): Boolean {
        if (query.isBlank()) return true
        return resolveAppName(event).contains(query, ignoreCase = true) ||
            event.attributedPackage?.contains(query, ignoreCase = true) == true ||
            WakeEventIdentity.effectivePackage(event)?.contains(query, ignoreCase = true) == true ||
            event.displayChannel?.contains(query, ignoreCase = true) == true
    }
}
