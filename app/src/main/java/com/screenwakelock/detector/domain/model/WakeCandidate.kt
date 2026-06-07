package com.screenwakelock.detector.domain.model

data class WakeCandidate(
    val packageName: String,
    val appLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val reasonCode: ReasonCode,
    val confidence: Float,
    val detail: String? = null,
)
