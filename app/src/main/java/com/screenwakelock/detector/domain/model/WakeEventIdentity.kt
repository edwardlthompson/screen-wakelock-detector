package com.screenwakelock.detector.domain.model

import com.screenwakelock.detector.domain.attributor.PackageFromWakelockTag

object WakeEventIdentity {
    fun effectivePackage(event: WakeEvent): String? =
        event.attributedPackage
            ?: PackageFromWakelockTag.extractPackage(event.wakelockTag)

    fun isIgnored(event: WakeEvent, ignoredPackages: Set<String>): Boolean {
        val pkg = effectivePackage(event) ?: return false
        return pkg in ignoredPackages
    }
}
