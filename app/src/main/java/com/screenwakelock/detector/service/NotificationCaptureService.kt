package com.screenwakelock.detector.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.hardware.display.DisplayManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Display
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot
import com.screenwakelock.detector.wakeshield.ShieldExemptPackages
import com.screenwakelock.detector.wakeshield.ShieldHardExemptResolver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject lateinit var notificationCacheRepository: NotificationCacheRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var hardExemptResolver: ShieldHardExemptResolver

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
            maybeShieldCancel(sbn, notification, importance, hasFullScreenIntent, hasTurnScreenOn)
        }
    }

    private suspend fun maybeShieldCancel(
        sbn: StatusBarNotification,
        notification: Notification,
        importance: Int,
        hasFullScreenIntent: Boolean,
        hasTurnScreenOn: Boolean,
    ) {
        if (!preferencesRepository.shieldEnabled.first()) return
        if (!isDisplayOffOrKeyguardLocked()) return
        val category = notification.category
        if (category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_ALARM) {
            return
        }
        val exempt = hardExemptResolver.resolve() +
            preferencesRepository.shieldAllowlistPackages.first()
        if (sbn.packageName in exempt || ShieldExemptPackages.isStaticExempt(sbn.packageName)) {
            return
        }
        if (hasFullScreenIntent) return
        val wakeCapable = hasTurnScreenOn || importance >= NotificationManager.IMPORTANCE_HIGH
        if (!wakeCapable) return
        cancelNotification(sbn.key)
        Log.i(TAG, "Shield L1 cancelled wake-capable notif pkg=${sbn.packageName}")
    }

    private fun isDisplayOffOrKeyguardLocked(): Boolean {
        val kg = getSystemService(KeyguardManager::class.java)
        if (kg?.isKeyguardLocked == true) return true
        val dm = getSystemService(DisplayManager::class.java) ?: return false
        val state = dm.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: return false
        return state == Display.STATE_OFF || state == Display.STATE_DOZE ||
            state == Display.STATE_DOZE_SUSPEND
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

        /**
         * Cancel wake-capable notifications except exempt packages and CALL/ALARM categories.
         */
        fun cancelWakeCapableExcept(exemptPackages: Set<String>): Int {
            val svc = instance ?: return 0
            var count = 0
            svc.activeNotifications?.forEach { sbn ->
                val notification = sbn.notification ?: return@forEach
                val category = notification.category
                if (category == Notification.CATEGORY_CALL ||
                    category == Notification.CATEGORY_ALARM
                ) {
                    return@forEach
                }
                if (sbn.packageName in exemptPackages) return@forEach
                if (ShieldExemptPackages.isStaticExempt(sbn.packageName)) return@forEach
                val snap = svc.toSnapshot(sbn) ?: return@forEach
                if (snap.hasFullScreenIntent) return@forEach
                val wakeCapable = snap.hasTurnScreenOn ||
                    snap.importance >= NotificationManager.IMPORTANCE_HIGH
                if (!wakeCapable) return@forEach
                svc.cancelNotification(sbn.key)
                count++
            }
            return count
        }
    }
}
