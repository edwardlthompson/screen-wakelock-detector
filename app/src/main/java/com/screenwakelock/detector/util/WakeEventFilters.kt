package com.screenwakelock.detector.util

import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.domain.model.WakeEventIdentity

object WakeEventFilters {
    fun isVisibleInLists(event: WakeEvent, ignoredPackages: Set<String>): Boolean =
        !WakeEventIdentity.isIgnored(event, ignoredPackages)

    fun filterVisible(events: List<WakeEvent>, ignoredPackages: Set<String>): List<WakeEvent> =
        events.filter { isVisibleInLists(it, ignoredPackages) }
}
