package com.screenwakelock.detector.domain.model

data class CachedNotification(
    val id: Long = 0,
    val packageName: String,
    val channelId: String?,
    val channelName: String?,
    val postedAtMillis: Long,
    val category: String?,
    val importance: Int,
)
