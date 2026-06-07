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
}
