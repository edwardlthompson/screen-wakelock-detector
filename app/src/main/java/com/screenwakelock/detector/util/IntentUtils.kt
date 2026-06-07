package com.screenwakelock.detector.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.screenwakelock.detector.MainActivity

object IntentUtils {
    const val DEEP_LINK_SCHEME = "screenwakelock"
    const val DEEP_LINK_HOST = "app"

    fun openApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun wakeDetailDeepLinkString(wakeEventId: Long): String =
        "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/detail/$wakeEventId"

    fun wakeDetailDeepLink(wakeEventId: Long): Uri =
        Uri.parse(wakeDetailDeepLinkString(wakeEventId))

    fun quickFixDeepLinkString(wakeEventId: Long): String =
        "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/quickfix/$wakeEventId"

    fun quickFixDeepLink(wakeEventId: Long): Uri =
        Uri.parse(quickFixDeepLinkString(wakeEventId))

    fun latestQuickFixDeepLinkString(): String =
        "$DEEP_LINK_SCHEME://wake/latest/actions"

    fun latestQuickFixDeepLink(): Uri =
        Uri.parse(latestQuickFixDeepLinkString())

    fun wakeLatestDeepLinkString(): String =
        "$DEEP_LINK_SCHEME://wake/latest"

    fun wakeLatestDeepLink(): Uri =
        Uri.parse(wakeLatestDeepLinkString())

    fun insightsHeatmapDeepLinkString(): String =
        "$DEEP_LINK_SCHEME://insights/heatmap"

    fun insightsHeatmapDeepLink(): Uri =
        Uri.parse(insightsHeatmapDeepLinkString())

    fun permissionsDeepLinkString(highlight: String? = null): String {
        val base = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/permissions"
        return if (highlight != null) "$base?highlight=$highlight" else base
    }

    fun permissionsDeepLink(highlight: String? = null): Uri =
        Uri.parse(permissionsDeepLinkString(highlight))

    fun notificationListenerSettings(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun usageAccessSettings(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun appNotificationSettings(packageName: String): Intent {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        return intent
    }

    fun channelNotificationSettings(packageName: String, channelId: String): Intent {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
        return intent
    }

    fun appDetailsSettings(packageName: String): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:$packageName")
        }
        return intent
    }

    fun batteryOptimizationSettings(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun requestIgnoreBatteryOptimizations(context: Context): Intent {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return intent
    }

    fun canOpenChannelSettings(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
