package com.screenwakelock.detector.service

import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject lateinit var notificationCacheRepository: NotificationCacheRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId
        } else {
            null
        }
        val channelName = runCatching {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.getNotificationChannel(channelId)?.name?.toString()
        }.getOrNull()
        val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId != null) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.getNotificationChannel(channelId)?.importance
                ?: NotificationManager.IMPORTANCE_DEFAULT
        } else {
            @Suppress("DEPRECATION")
            notification.priority
        }

        scope.launch {
            notificationCacheRepository.cacheNotification(
                packageName = sbn.packageName,
                channelId = channelId,
                channelName = channelName,
                postedAtMillis = sbn.postTime,
                category = notification.category,
                importance = importance,
            )
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        scope.launch {
            activeNotifications?.forEach { sbn ->
                onNotificationPosted(sbn)
            }
        }
    }

    fun dismissMatching(packageName: String, channelId: String?): Int {
        var count = 0
        activeNotifications?.forEach { sbn ->
            val matchesPackage = sbn.packageName == packageName
            val sbnChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sbn.notification.channelId
            } else {
                null
            }
            val matchesChannel = channelId == null || sbnChannel == channelId
            if (matchesPackage && matchesChannel) {
                cancelNotification(sbn.key)
                count++
            }
        }
        return count
    }

    companion object {
        @Volatile
        private var instance: NotificationCaptureService? = null

        fun dismissNotifications(packageName: String, channelId: String?): Int =
            instance?.dismissMatching(packageName, channelId) ?: 0
    }
}
