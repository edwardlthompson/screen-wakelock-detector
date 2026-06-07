package com.screenwakelock.detector.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WakeEventEntity): Long

    @Query("SELECT * FROM wake_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<WakeEventEntity>>

    @Query("SELECT * FROM wake_events ORDER BY timestampMillis DESC LIMIT 1")
    fun observeLatest(): Flow<WakeEventEntity?>

    @Query("SELECT * FROM wake_events ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestOnce(): WakeEventEntity?

    @Query("SELECT * FROM wake_events WHERE id = :id")
    suspend fun getById(id: Long): WakeEventEntity?

    @Query("SELECT * FROM wake_events WHERE id = :id")
    fun observeById(id: Long): Flow<WakeEventEntity?>

    @Query("SELECT * FROM wake_events ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<WakeEventEntity>

    @Query(
        """
        SELECT * FROM wake_events
        WHERE timestampMillis >= :sinceMillis
        ORDER BY timestampMillis DESC
        """,
    )
    suspend fun getSince(sinceMillis: Long): List<WakeEventEntity>

    @Query("DELETE FROM wake_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM wake_events")
    suspend fun count(): Int

    @Query("DELETE FROM wake_events WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long): Int

    @Query(
        """
        SELECT * FROM wake_events
        WHERE attributedPackage = :packageName
          AND (channelId IS :channelId OR (channelId IS NULL AND :channelId IS NULL))
          AND timestampMillis >= :sinceMillis
          AND id != :excludeId
        ORDER BY timestampMillis DESC
        LIMIT :limit
        """,
    )
    suspend fun findSimilar(
        packageName: String,
        channelId: String?,
        sinceMillis: Long,
        excludeId: Long,
        limit: Int,
    ): List<WakeEventEntity>

    @Query(
        """
        SELECT * FROM wake_events
        WHERE attributedPackage = :packageName
          AND rootEnhanced = 1
          AND timestampMillis >= :sinceMillis
        ORDER BY timestampMillis DESC
        LIMIT :limit
        """,
    )
    suspend fun getRootEnhancedForPackageSince(
        packageName: String,
        sinceMillis: Long,
        limit: Int,
    ): List<WakeEventEntity>
}
