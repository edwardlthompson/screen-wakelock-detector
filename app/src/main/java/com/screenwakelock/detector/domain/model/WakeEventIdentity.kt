package com.screenwakelock.detector.domain.model

import com.screenwakelock.detector.domain.attributor.PackageFromWakelockTag
import com.screenwakelock.detector.util.TimeUtils

data class IgnorePolicy(
    val always: Set<String> = emptySet(),
    val nightOnly: Set<String> = emptySet(),
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 6,
)

object WakeEventIdentity {
    fun effectivePackage(event: WakeEvent): String? =
        event.attributedPackage
            ?: PackageFromWakelockTag.extractPackage(event.wakelockTag)

    fun isIgnored(event: WakeEvent, ignoredPackages: Set<String>): Boolean =
        isIgnored(event, IgnorePolicy(always = ignoredPackages))

    fun isIgnored(event: WakeEvent, policy: IgnorePolicy): Boolean {
        val pkg = effectivePackage(event) ?: return false
        if (pkg in policy.always) return true
        if (pkg in policy.nightOnly &&
            TimeUtils.isNighttime(event.timestampMillis, policy.nightStartHour, policy.nightEndHour)
        ) {
            return true
        }
        return false
    }
}
