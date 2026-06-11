package com.screenwakelock.detector.util

import com.screenwakelock.detector.domain.model.WakeEvent

object WakeEventFilters {
    fun isVisibleInLists(event: WakeEvent, ignoredPackages: Set<String>): Boolean =
        event.attributedPackage == null || event.attributedPackage !in ignoredPackages

    fun filterVisible(events: List<WakeEvent>, ignoredPackages: Set<String>): List<WakeEvent> =
        events.filter { isVisibleInLists(it, ignoredPackages) }
}
