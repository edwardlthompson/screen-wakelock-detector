package com.screenwakelock.detector.worker

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import com.screenwakelock.detector.data.settingsDataStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screenwakelock.detector.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class RetentionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val retentionDays = applicationContext.settingsDataStore.data
            .map { it[RETENTION_DAYS_KEY] ?: 0 }
            .first()
        if (retentionDays <= 0) return Result.success()

        val cutoff = System.currentTimeMillis() - retentionDays.toLong() * 86_400_000L
        val db = AppDatabase.getInstance(applicationContext)
        db.wakeEventDao().deleteBefore(cutoff)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "retention_prune"
        private val RETENTION_DAYS_KEY = intPreferencesKey("retention_days")

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        suspend fun pruneNow(context: Context) {
            val retentionDays = context.settingsDataStore.data
                .map { it[RETENTION_DAYS_KEY] ?: 0 }
                .first()
            if (retentionDays <= 0) return
            val cutoff = System.currentTimeMillis() - retentionDays.toLong() * 86_400_000L
            AppDatabase.getInstance(context).wakeEventDao().deleteBefore(cutoff)
        }
    }
}
