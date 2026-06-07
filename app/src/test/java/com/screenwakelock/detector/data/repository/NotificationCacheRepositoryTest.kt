package com.screenwakelock.detector.data.repository

import com.screenwakelock.detector.data.db.NotificationCacheDao
import com.screenwakelock.detector.data.db.NotificationCacheEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCacheRepositoryTest {

    @Test
    fun getInWindow_returnsNotificationsWithinCorrelationRange() = runBlocking {
        val dao = FakeNotificationCacheDao()
        val repository = NotificationCacheRepository(dao)
        val postedAt = 1_700_000_000_000L
        repository.cacheNotification(
            packageName = "com.example.app",
            channelId = "alerts",
            channelName = "Alerts",
            postedAtMillis = postedAt,
            category = null,
            importance = 4,
        )

        val results = repository.getInWindow(
            startMillis = postedAt - WakeAttributorWindow.WINDOW_MS,
            endMillis = postedAt + WakeAttributorWindow.WINDOW_MS,
        )

        assertEquals(1, results.size)
        assertEquals("com.example.app", results.first().packageName)
        assertEquals("alerts", results.first().channelId)
    }

    @Test
    fun getInWindow_excludesNotificationsOutsideWindow() = runBlocking {
        val dao = FakeNotificationCacheDao()
        val repository = NotificationCacheRepository(dao)
        repository.cacheNotification(
            packageName = "com.old.app",
            channelId = "old",
            channelName = null,
            postedAtMillis = 1_000L,
            category = null,
            importance = 3,
        )

        val results = repository.getInWindow(
            startMillis = 5_000L,
            endMillis = 10_000L,
        )

        assertEquals(0, results.size)
    }
}

/** Mirrors WakeAttributor.DEFAULT_WINDOW_MS without pulling Android Context into tests. */
private object WakeAttributorWindow {
    const val WINDOW_MS = 5_000L
}

private class FakeNotificationCacheDao : NotificationCacheDao {
    private val store = mutableListOf<NotificationCacheEntity>()
    private var nextId = 1L

    override suspend fun insert(entry: NotificationCacheEntity): Long {
        val id = if (entry.id == 0L) nextId++ else entry.id
        store.add(entry.copy(id = id))
        return id
    }

    override suspend fun getInWindow(
        startMillis: Long,
        endMillis: Long,
    ): List<NotificationCacheEntity> =
        store.filter { it.postedAtMillis in startMillis..endMillis }

    override suspend fun deleteOlderThan(cutoffMillis: Long) {
        store.removeAll { it.postedAtMillis < cutoffMillis }
    }

    override suspend fun count(): Int = store.size
}
