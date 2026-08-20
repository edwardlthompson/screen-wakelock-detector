package com.screenwakelock.detector.worker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screenwakelock.detector.alerts.WakeAlertNotifier
import com.screenwakelock.detector.data.db.AppDatabase
import com.screenwakelock.detector.data.db.toDomain
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.data.settingsDataStore
import com.screenwakelock.detector.domain.insights.ShieldDigestCopy
import com.screenwakelock.detector.domain.insights.ShieldWeekCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class ShieldDigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = applicationContext.settingsDataStore.data
            .map { it[DIGEST_ENABLED_KEY] ?: true }
            .first()
        if (!enabled) return Result.success()
        if (!PermissionStatusRepository(applicationContext).isPostNotificationsGranted()) {
            return Result.success()
        }

        val events = AppDatabase.getInstance(applicationContext)
            .wakeEventDao()
            .getAll()
            .map { it.toDomain() }
        val counts = ShieldWeekCalculator.summarize(events)
        if (!counts.hasData) return Result.success()

        WakeAlertNotifier.notifyWeeklyShieldDigest(
            applicationContext,
            ShieldDigestCopy.body(counts),
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "shield_weekly_digest"
        private val DIGEST_ENABLED_KEY = booleanPreferencesKey("shield_digest_enabled")

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ShieldDigestWorker>(7, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
