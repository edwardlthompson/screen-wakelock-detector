package com.screenwakelock.detector.data.repository

import com.screenwakelock.detector.data.db.NotificationCacheDao
import com.screenwakelock.detector.data.db.NotificationCacheEntity
import com.screenwakelock.detector.data.db.toDomain
import com.screenwakelock.detector.domain.model.CachedNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationCacheRepository @Inject constructor(
    private val notificationCacheDao: NotificationCacheDao,
) {
    suspend fun cacheNotification(
        packageName: String,
        channelId: String?,
        channelName: String?,
        postedAtMillis: Long,
        category: String?,
        importance: Int,
    ) {
        notificationCacheDao.insert(
            NotificationCacheEntity(
                packageName = packageName,
                channelId = channelId,
                channelName = channelName,
                postedAtMillis = postedAtMillis,
                category = category,
                importance = importance,
            ),
        )
    }

    suspend fun getInWindow(startMillis: Long, endMillis: Long): List<CachedNotification> =
        notificationCacheDao.getInWindow(startMillis, endMillis).map { it.toDomain() }

    suspend fun pruneOlderThan(cutoffMillis: Long) {
        notificationCacheDao.deleteOlderThan(cutoffMillis)
    }
}
