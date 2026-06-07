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

    val displayAppName: String
        get() = attributedAppLabel ?: attributedPackage ?: "Unknown app"

    val displayChannel: String?
        get() = channelName ?: channelId
}
