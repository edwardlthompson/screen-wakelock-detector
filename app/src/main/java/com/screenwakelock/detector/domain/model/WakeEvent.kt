package com.screenwakelock.detector.domain.model

data class WakeEvent(
    val id: Long = 0,
    val timestampMillis: Long,
    val attributedPackage: String?,
    val attributedAppLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val reasonCode: ReasonCode,
    val confidence: Float,
    val candidates: List<WakeCandidate> = emptyList(),
    val rootEnhanced: Boolean = false,
    val wakelockTag: String? = null,
    val wakelockName: String? = null,
    val rootParserId: String? = null,
    val screenOffDurationMs: Long? = null,
) {
    val isLowConfidence: Boolean get() = confidence < 0.6f || reasonCode == ReasonCode.UNKNOWN

    /**
     * Offline display name without PackageManager lookups.
     * User-visible UI must use [com.screenwakelock.detector.domain.attributor.AppDisplayResolver].
     */
    val displayAppName: String
        get() = WakeEventDisplayNames.offlineAppName(this)

    val displayChannel: String?
        get() = channelName ?: channelId
}
