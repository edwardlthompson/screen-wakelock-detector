package com.screenwakelock.detector.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_cache",
    indices = [Index(value = ["postedAtMillis"])],
)
data class NotificationCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val channelId: String?,
    val channelName: String?,
    val postedAtMillis: Long,
    val category: String?,
    val importance: Int,
)
