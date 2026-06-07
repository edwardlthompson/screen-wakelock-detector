package com.screenwakelock.detector.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_events")
data class WakeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val attributedPackage: String?,
    val attributedAppLabel: String?,
    val channelId: String?,
    val channelName: String?,
    val reasonCode: String,
    val confidence: Float,
    val candidatesJson: String?,
    val rootEnhanced: Boolean = false,
    val wakelockTag: String? = null,
    val wakelockName: String? = null,
    val screenOffDurationMs: Long? = null,
)
