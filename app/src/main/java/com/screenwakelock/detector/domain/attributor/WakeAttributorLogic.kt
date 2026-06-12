package com.screenwakelock.detector.domain.attributor

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeCandidate
import com.screenwakelock.detector.root.RootSnapshot

fun capUsageCandidateConfidence(
    candidates: List<WakeCandidate>,
    hasNotifications: Boolean,
): List<WakeCandidate> =
    if (hasNotifications) {
        candidates.map { it.copy(confidence = it.confidence.coerceAtMost(0.45f)) }
    } else {
        candidates
    }

fun rootWakeCandidate(
    snapshot: RootSnapshot,
    resolveLabel: (String) -> String?,
): WakeCandidate? {
    val rootPackage = snapshot.packageName
        ?: PackageFromWakelockTag.extractPackage(snapshot.wakelockTag)
        ?: return null
    return WakeCandidate(
        packageName = rootPackage,
        appLabel = resolveLabel(rootPackage),
        channelId = null,
        channelName = null,
        reasonCode = snapshot.reasonCode ?: ReasonCode.ROOT_WAKELOCK,
        confidence = if (snapshot.packageName != null) 0.85f else 0.55f,
        detail = snapshot.wakelockTag,
    )
}
