package com.screenwakelock.detector.service

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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId
        } else {
            null
        }
        val channelName = runCatching {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.getNotificationChannel(channelId)?.name?.toString()
        }.getOrNull()

        scope.launch {
            notificationCacheRepository.cacheNotification(
                packageName = sbn.packageName,
                channelId = channelId,
                channelName = channelName,
                postedAtMillis = sbn.postTime,
                category = notification.category,
                importance = notification.priority,
            )
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope.launch {
            activeNotifications?.forEach { sbn ->
                onNotificationPosted(sbn)
            }
        }
    }
}
