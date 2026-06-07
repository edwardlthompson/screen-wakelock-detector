package com.screenwakelock.detector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.screenwakelock.detector.MainActivity
import com.screenwakelock.detector.R
import com.screenwakelock.detector.alerts.WakeAlertNotifier
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.data.repository.WakeEventRepository
import com.screenwakelock.detector.domain.attributor.WakeAttributor
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.widget.WakeWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WakeMonitorService : LifecycleService() {

    @Inject lateinit var wakeEventRepository: WakeEventRepository
    @Inject lateinit var notificationCacheRepository: NotificationCacheRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var wakeAttributor: WakeAttributor
    @Inject lateinit var wakeAlertNotifier: WakeAlertNotifier
    @Inject lateinit var callbackHolder: WakeMonitorCallbackHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var screenOffMillis: Long? = null
    private var receiver: WakeMonitorReceiver? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        receiver = WakeMonitorReceiver(callbackHolder).also {
            WakeMonitorReceiver.register(this, it)
        }
        callbackHolder.onScreenOn = { serviceScope.launch { handleScreenOn() } }
        callbackHolder.onScreenOff = { screenOffMillis = System.currentTimeMillis() }
        serviceScope.launch {
            notificationCacheRepository.pruneOlderThan(
                System.currentTimeMillis() - NOTIFICATION_RETENTION_MS,
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForegroundWithNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        callbackHolder.onScreenOn = null
        callbackHolder.onScreenOff = null
        receiver?.let { WakeMonitorReceiver.unregister(this, it) }
        receiver = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private suspend fun handleScreenOn() {
        val now = System.currentTimeMillis()
        val rootEnabled = preferencesRepository.rootEnabled.first()
        val attribution = wakeAttributor.attribute(
            screenOnMillis = now,
            notificationCache = notificationCacheRepository,
            rootEnabled = rootEnabled,
        )
        val event = WakeEvent(
            timestampMillis = now,
            attributedPackage = attribution.packageName,
            attributedAppLabel = attribution.appLabel,
            channelId = attribution.channelId,
            channelName = attribution.channelName,
            reasonCode = attribution.reasonCode,
            confidence = attribution.confidence,
            candidates = attribution.candidates,
            rootEnhanced = attribution.rootEnhanced,
            wakelockTag = attribution.wakelockTag,
            wakelockName = attribution.wakelockName,
            screenOffDurationMs = screenOffMillis?.let { now - it },
        )
        val id = wakeEventRepository.insert(event)
        WakeWidgetReceiver.requestUpdate(this)

        val alertEvery = preferencesRepository.alertOnEveryWake.first()
        val thresholdEnabled = preferencesRepository.thresholdAlertsEnabled.first()
        if (alertEvery) {
            wakeAlertNotifier.notifySingleWake(event.copy(id = id))
        } else if (thresholdEnabled) {
            wakeAlertNotifier.maybeNotifyThreshold(
                event.copy(id = id),
                wakeEventRepository,
                preferencesRepository.thresholdCount.first(),
            )
        }
    }

    private fun startForegroundWithNotification() {
        createChannel()
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setContentText(getString(R.string.monitoring_notification_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitoring_notification_title),
                NotificationManager.IMPORTANCE_LOW,
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "wake_monitor"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.screenwakelock.detector.STOP_MONITOR"
        private const val NOTIFICATION_RETENTION_MS = 86_400_000L

        fun start(context: android.content.Context) {
            val intent = Intent(context, WakeMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.startService(
                Intent(context, WakeMonitorService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
