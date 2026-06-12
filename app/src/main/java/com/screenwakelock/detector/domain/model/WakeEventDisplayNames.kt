package com.screenwakelock.detector.domain.model

import com.screenwakelock.detector.domain.attributor.AppDisplayResolver
import com.screenwakelock.detector.domain.attributor.PackageFromWakelockTag

/**
 * Display-name helpers that do not require [android.content.Context] or PackageManager.
 *
 * For user-visible strings in UI, widgets, and alerts, use [AppDisplayResolver] instead —
 * it applies fresh PM labels on top of these offline fallbacks.
 */
object WakeEventDisplayNames {
    const val UNKNOWN: String = AppDisplayResolver.UNKNOWN_APP_LABEL

    /** Offline app name: label → package → tag-derived package → tag → wakelock name → unknown. */
    fun offlineAppName(event: WakeEvent): String {
        event.attributedAppLabel?.let { return it }
        event.attributedPackage?.let { return it }
        PackageFromWakelockTag.extractPackage(event.wakelockTag)?.let { return it }
        event.wakelockTag?.let { return it }
        event.wakelockName?.let { return it }
        return UNKNOWN
    }
}
