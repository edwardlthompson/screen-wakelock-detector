package com.screenwakelock.detector.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NotificationCacheEntity): Long

    @Query(
        """
        SELECT * FROM notification_cache
        WHERE postedAtMillis BETWEEN :startMillis AND :endMillis
        ORDER BY postedAtMillis DESC
        """,
    )
    suspend fun getInWindow(startMillis: Long, endMillis: Long): List<NotificationCacheEntity>

    @Query(
        """
        DELETE FROM notification_cache
        WHERE postedAtMillis < :cutoffMillis
        """,
    )
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM notification_cache")
    suspend fun count(): Int
}
