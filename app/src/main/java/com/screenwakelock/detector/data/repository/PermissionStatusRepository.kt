package com.screenwakelock.detector.data.repository

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.domain.model.PermissionStatus
import com.screenwakelock.detector.service.NotificationCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, NotificationCaptureService::class.java)
        return enabled.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
    }

    fun isUsageStatsGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isPostNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getAllStatuses(): List<PermissionStatus> = listOf(
        PermissionStatus(
            kind = PermissionKind.NOTIFICATION_LISTENER,
            granted = isNotificationListenerEnabled(),
            label = "Notification access",
            description = "Why: match screen wakes to the app and channel that posted nearby. " +
                "What: package name, channel ID, importance, timestamp (metadata only). " +
                "Never accesses: notification message text or contact names.",
        ),
        PermissionStatus(
            kind = PermissionKind.USAGE_STATS,
            granted = isUsageStatsGranted(),
            label = "Usage access",
            description = "Why: fallback when no notification matches the wake window. " +
                "What: which app was recently in foreground. Never accesses: app content or files.",
        ),
        PermissionStatus(
            kind = PermissionKind.POST_NOTIFICATIONS,
            granted = isPostNotificationsGranted(),
            label = "Alert notifications",
            description = "Show threshold and wake alerts from this app",
        ),
        PermissionStatus(
            kind = PermissionKind.BATTERY_OPTIMIZATION,
            granted = isBatteryOptimizationIgnored(),
            label = "Battery unrestricted",
            description = "Helps monitoring survive Doze and background limits",
        ),
    )

    fun missingCount(): Int = getAllStatuses().count { !it.granted }

    /** Four permissions, 25% each — 100 when all granted. */
    fun healthScore(): Int {
        val statuses = getAllStatuses()
        if (statuses.isEmpty()) return 100
        val granted = statuses.count { it.granted }
        return (granted * 100) / statuses.size
    }
}
