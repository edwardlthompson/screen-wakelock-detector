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

    fun wakeDetailDeepLink(wakeEventId: Long): Uri =
        Uri.parse("$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/detail/$wakeEventId")

    fun quickFixDeepLink(wakeEventId: Long): Uri =
        Uri.parse("$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/quickfix/$wakeEventId")

    fun permissionsDeepLink(highlight: String? = null): Uri {
        val base = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/permissions"
        return if (highlight != null) {
            Uri.parse("$base?highlight=$highlight")
        } else {
            Uri.parse(base)
        }
    }

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
