package com.screenwakelock.detector.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.screenwakelock.detector.MainActivity
import com.screenwakelock.detector.R
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.data.repository.WakeEventRepository
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val permissionRepo = PermissionStatusRepository(context)

    init {
        createChannel()
    }

    fun notifySingleWake(event: WakeEvent) {
        if (!permissionRepo.isPostNotificationsGranted()) return
        val appName = event.displayAppName
        val channel = event.displayChannel ?: "Unknown channel"
        val body = "$channel · ${event.reasonCode.friendlyLabel()}"
        showNotification(
            id = (event.id % Int.MAX_VALUE).toInt(),
            title = "Screen woke — $appName",
            text = body,
            wakeEventId = event.id,
        )
    }

    suspend fun maybeNotifyThreshold(
        event: WakeEvent,
        wakeEventRepository: WakeEventRepository,
        threshold: Int,
    ) {
        if (!permissionRepo.isPostNotificationsGranted()) return
        val pkg = event.attributedPackage ?: return
        val channelId = event.channelId
        val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        val recent = wakeEventRepository.getSince(since)
        val count = recent.count {
            it.attributedPackage == pkg && it.channelId == channelId
        }
        if (count < threshold) return

        val appName = event.displayAppName
        val channelName = event.displayChannel ?: "Unknown"
        val body = "$channelName · $count times in the last hour — last at " +
            TimeUtils.formatTime(event.timestampMillis)
        showNotification(
            id = (pkg.hashCode() + (channelId?.hashCode() ?: 0)),
            title = "$appName woke your screen",
            text = body,
            wakeEventId = event.id,
        )
    }

    fun notifyUnknownWake(event: WakeEvent) {
        if (!permissionRepo.isPostNotificationsGranted()) return
        val time = TimeUtils.formatTime(event.timestampMillis)
        val (title, body, highlight) = when {
            !permissionRepo.isNotificationListenerEnabled() -> Triple(
                "Screen woke — app unknown",
                "Your screen turned on at $time. Enable Notification access to identify which app caused it.",
                "notification_access",
            )
            !permissionRepo.isUsageStatsGranted() -> Triple(
                "Screen woke — cause unclear",
                "No matching notification found. Enable Usage access to improve detection.",
                "usage_access",
            )
            permissionRepo.missingCount() > 1 -> Triple(
                "Screen woke — setup incomplete",
                "${permissionRepo.missingCount()} permissions off. Turn on permissions in Settings.",
                null,
            )
            else -> Triple(
                "Screen woke — app unknown",
                "Your screen turned on at $time. Cause could not be determined.",
                null,
            )
        }
        showNotification(
            id = UNKNOWN_ALERT_ID,
            title = title,
            text = body,
            wakeEventId = event.id,
            highlight = highlight,
        )
    }

    private fun showNotification(
        id: Int,
        title: String,
        text: String,
        wakeEventId: Long,
        highlight: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = IntentUtils.quickFixDeepLink(wakeEventId)
            if (highlight != null) {
                data = IntentUtils.permissionsDeepLink(highlight)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(id, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Wake alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val ALERT_CHANNEL_ID = "wake_alerts"
        const val GROUP_KEY = "wake_alerts_group"
        const val UNKNOWN_ALERT_ID = 2001
    }
}
