package com.screenwakelock.detector.domain.model

data class AttributionData(
    val packageName: String?,
    val appLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val reasonCode: ReasonCode,
    val confidence: Float,
    val candidates: List<WakeCandidate> = emptyList(),
    val rootEnhanced: Boolean = false,
    val wakelockTag: String? = null,
    val wakelockName: String? = null,
)
