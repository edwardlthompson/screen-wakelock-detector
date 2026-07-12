package com.screenwakelock.detector.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        val channelId = channelIdOf(notification)
        val channelName = channelNameOf(channelId)
        val importance = importanceOf(notification, channelId)
        val observedAtMillis = System.currentTimeMillis()
        val hasFullScreenIntent = notification.fullScreenIntent != null
        val hasTurnScreenOn = (notification.flags and NotificationFlagTurnScreenOn) != 0

        // Synchronous write so WakeAttributor.cache window reads see this post.
        runBlocking(Dispatchers.IO) {
            notificationCacheRepository.cacheNotification(
                packageName = sbn.packageName,
                channelId = channelId,
                channelName = channelName,
                postedAtMillis = observedAtMillis,
                category = notification.category,
                importance = importance,
                hasFullScreenIntent = hasFullScreenIntent,
                hasTurnScreenOn = hasTurnScreenOn,
            )
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
        }
        Log.w(
            TAG,
            "Notification listener disconnected — attribution may miss notifications until reconnected",
        )
        runCatching {
            requestRebind(ComponentName(this, NotificationCaptureService::class.java))
        }.onFailure { err ->
            Log.w(TAG, "requestRebind failed: ${err.message}")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Notification listener connected")
        scope.launch {
            activeNotifications?.forEach { sbn ->
                onNotificationPosted(sbn)
            }
        }
    }

    private fun toSnapshot(sbn: StatusBarNotification): ActiveNotificationSnapshot? {
        val notification = sbn.notification ?: return null
        val channelId = channelIdOf(notification)
        return ActiveNotificationSnapshot(
            packageName = sbn.packageName,
            channelId = channelId,
            channelName = channelNameOf(channelId),
            category = notification.category,
            importance = importanceOf(notification, channelId),
            hasFullScreenIntent = notification.fullScreenIntent != null,
            hasTurnScreenOn = (notification.flags and NotificationFlagTurnScreenOn) != 0,
        )
    }

    private fun channelIdOf(notification: Notification): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId
        } else {
            null
        }

    private fun channelNameOf(channelId: String?): String? = runCatching {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.getNotificationChannel(channelId)?.name?.toString()
    }.getOrNull()

    private fun importanceOf(notification: Notification, channelId: String?): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId != null) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.getNotificationChannel(channelId)?.importance
                ?: NotificationManager.IMPORTANCE_DEFAULT
        } else {
            @Suppress("DEPRECATION")
            notification.priority
        }

    fun dismissMatching(packageName: String, channelId: String?): Int {
        var count = 0
        activeNotifications?.forEach { sbn ->
            val matchesPackage = sbn.packageName == packageName
            val sbnChannel = sbn.notification?.let { channelIdOf(it) }
            val matchesChannel = channelId == null || sbnChannel == channelId
            if (matchesPackage && matchesChannel) {
                cancelNotification(sbn.key)
                count++
            }
        }
        return count
    }

    companion object {
        private const val TAG = "NotificationCapture"
        /** [Notification] flag to turn the screen on when posted (API 27+). */
        private const val NotificationFlagTurnScreenOn = 1 shl 19

        @Volatile
        private var instance: NotificationCaptureService? = null

        fun isListenerBound(): Boolean = instance != null

        fun dismissNotifications(packageName: String, channelId: String?): Int =
            instance?.dismissMatching(packageName, channelId) ?: 0

        fun snapshotActiveNotifications(): List<ActiveNotificationSnapshot> {
            val svc = instance ?: return emptyList()
            return svc.activeNotifications?.mapNotNull { svc.toSnapshot(it) } ?: emptyList()
        }
    }
}
