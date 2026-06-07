package com.screenwakelock.detector.data.repository

import com.screenwakelock.detector.data.db.WakeEventDao
import com.screenwakelock.detector.data.db.toDomain
import com.screenwakelock.detector.data.db.toEntity
import com.screenwakelock.detector.domain.model.WakeEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeEventRepository @Inject constructor(
    private val wakeEventDao: WakeEventDao,
) {
    fun observeAll(): Flow<List<WakeEvent>> =
        wakeEventDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeLatest(): Flow<WakeEvent?> =
        wakeEventDao.observeLatest().map { entity -> entity?.toDomain() }

    fun observeById(id: Long): Flow<WakeEvent?> =
        wakeEventDao.observeById(id).map { entity -> entity?.toDomain() }

    suspend fun getById(id: Long): WakeEvent? =
        wakeEventDao.getById(id)?.toDomain()

    suspend fun insert(event: WakeEvent): Long =
        wakeEventDao.insert(event.toEntity())

    suspend fun getAll(): List<WakeEvent> =
        wakeEventDao.getAll().map { it.toDomain() }

    suspend fun getSince(sinceMillis: Long): List<WakeEvent> =
        wakeEventDao.getSince(sinceMillis).map { it.toDomain() }

    suspend fun deleteById(id: Long) = wakeEventDao.deleteById(id)

    suspend fun count(): Int = wakeEventDao.count()

    suspend fun deleteBefore(cutoffMillis: Long): Int = wakeEventDao.deleteBefore(cutoffMillis)

    suspend fun findSimilar(
        packageName: String,
        channelId: String?,
        sinceMillis: Long,
        excludeId: Long,
        limit: Int = 10,
    ): List<WakeEvent> =
        wakeEventDao.findSimilar(packageName, channelId, sinceMillis, excludeId, limit)
            .map { it.toDomain() }

    suspend fun getRootTimelineForPackage(
        packageName: String,
        sinceMillis: Long,
        limit: Int = 20,
    ): List<WakeEvent> =
        wakeEventDao.getRootEnhancedForPackageSince(packageName, sinceMillis, limit)
            .map { it.toDomain() }
}
