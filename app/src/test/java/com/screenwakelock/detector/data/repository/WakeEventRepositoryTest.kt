package com.screenwakelock.detector.data.repository

import com.screenwakelock.detector.data.db.WakeEventDao
import com.screenwakelock.detector.data.db.WakeEventEntity
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WakeEventRepositoryTest {

    @Test
    fun insert_andGetById_roundTripsDomainModel() = runBlocking {
        val dao = FakeWakeEventDao()
        val repository = WakeEventRepository(dao)
        val event = WakeEvent(
            timestampMillis = 1_700_000_000_000L,
            attributedPackage = "com.example.app",
            attributedAppLabel = "Example",
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.UNKNOWN,
            confidence = 0.2f,
        )

        val id = repository.insert(event)
        val loaded = repository.getById(id)

        assertNotNull(loaded)
        assertEquals(id, loaded!!.id)
        assertEquals("com.example.app", loaded.attributedPackage)
        assertEquals(ReasonCode.UNKNOWN, loaded.reasonCode)
    }

    @Test
    fun observeAll_mapsEntitiesToDomain() = runBlocking {
        val dao = FakeWakeEventDao()
        val repository = WakeEventRepository(dao)
        repository.insert(
            WakeEvent(
                timestampMillis = 100L,
                attributedPackage = "a",
                attributedAppLabel = null,
                channelId = null,
                channelName = null,
                reasonCode = ReasonCode.UNKNOWN,
                confidence = 0.1f,
            ),
        )

        val events = repository.observeAll().first()

        assertEquals(1, events.size)
        assertEquals("a", events.first().attributedPackage)
    }

    @Test
    fun deleteBefore_removesOldEvents() = runBlocking {
        val dao = FakeWakeEventDao()
        val repository = WakeEventRepository(dao)
        repository.insert(
            WakeEvent(
                timestampMillis = 100L,
                attributedPackage = "old",
                attributedAppLabel = null,
                channelId = null,
                channelName = null,
                reasonCode = ReasonCode.UNKNOWN,
                confidence = 0.1f,
            ),
        )
        repository.insert(
            WakeEvent(
                timestampMillis = 500L,
                attributedPackage = "new",
                attributedAppLabel = null,
                channelId = null,
                channelName = null,
                reasonCode = ReasonCode.UNKNOWN,
                confidence = 0.1f,
            ),
        )

        val deleted = repository.deleteBefore(300L)
        val remaining = repository.getAll()

        assertEquals(1, deleted)
        assertEquals(1, remaining.size)
        assertEquals("new", remaining.first().attributedPackage)
    }
}

private class FakeWakeEventDao : WakeEventDao {
    private val store = mutableListOf<WakeEventEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<WakeEventEntity>>(emptyList())

    override suspend fun insert(event: WakeEventEntity): Long {
        val id = if (event.id == 0L) nextId++ else event.id
        val saved = event.copy(id = id)
        store.removeAll { it.id == id }
        store.add(saved)
        flow.value = store.sortedByDescending { it.timestampMillis }
        return id
    }

    override fun observeAll(): Flow<List<WakeEventEntity>> = flow

    override fun observeLatest(): Flow<WakeEventEntity?> =
        flow.map { entities -> entities.firstOrNull() }

    override suspend fun getLatestOnce(): WakeEventEntity? =
        store.maxByOrNull { it.timestampMillis }

    override suspend fun getById(id: Long): WakeEventEntity? =
        store.find { it.id == id }

    override fun observeById(id: Long): Flow<WakeEventEntity?> =
        flow.map { entities -> entities.find { it.id == id } }

    override suspend fun getAll(): List<WakeEventEntity> =
        store.sortedByDescending { it.timestampMillis }

    override suspend fun getSince(sinceMillis: Long): List<WakeEventEntity> =
        store.filter { it.timestampMillis >= sinceMillis }

    override suspend fun deleteById(id: Long) {
        store.removeAll { it.id == id }
        flow.value = store.sortedByDescending { it.timestampMillis }
    }

    override suspend fun count(): Int = store.size

    override suspend fun deleteBefore(cutoffMillis: Long): Int {
        val before = store.size
        store.removeAll { it.timestampMillis < cutoffMillis }
        flow.value = store.sortedByDescending { it.timestampMillis }
        return before - store.size
    }

    override suspend fun findSimilar(
        packageName: String,
        channelId: String?,
        sinceMillis: Long,
        excludeId: Long,
        limit: Int,
    ): List<WakeEventEntity> =
        store.filter {
            it.attributedPackage == packageName &&
                it.channelId == channelId &&
                it.timestampMillis >= sinceMillis &&
                it.id != excludeId
        }
            .sortedByDescending { it.timestampMillis }
            .take(limit)

    override suspend fun getRootEnhancedForPackageSince(
        packageName: String,
        sinceMillis: Long,
        limit: Int,
    ): List<WakeEventEntity> =
        store.filter {
            it.attributedPackage == packageName &&
                it.rootEnhanced &&
                it.timestampMillis >= sinceMillis
        }
            .sortedByDescending { it.timestampMillis }
            .take(limit)
}
