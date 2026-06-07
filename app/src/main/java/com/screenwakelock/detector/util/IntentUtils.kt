package com.screenwakelock.detector.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.screenwakelock.detector.MainActivity
import com.screenwakelock.detector.service.NotificationCaptureService

object IntentUtils {
    private const val VENMO_PACKAGE = "com.venmo"
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

    /**
     * Opens this app's notification-access toggle directly (triggers the restricted-settings
     * dialog on sideloaded installs when the user enables the switch).
     */
    fun notificationListenerDetailSettings(context: Context): Intent {
        val component = ComponentName(context, NotificationCaptureService::class.java)
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        }
    }

    /** Intents to trigger the restricted-settings block dialog, most specific first. */
    fun restrictedSettingsTriggerIntents(context: Context): List<Intent> = listOf(
        notificationListenerDetailSettings(context),
        notificationListenerSettings(),
    )

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

    fun viewUri(context: Context, uri: String): Boolean =
        startViewIntent(context, buildViewIntent(uri))

    /** Prefer Venmo app for venmo.com links; fall back to browser or system chooser. */
    fun viewDonateUri(context: Context, uri: String): Boolean {
        if (uri.contains("venmo.com", ignoreCase = true)) {
            val venmoIntent = buildViewIntent(uri, VENMO_PACKAGE)
            if (startViewIntent(context, venmoIntent)) {
                return true
            }
        }
        return viewUri(context, uri)
    }

    internal fun buildViewIntent(uri: String, targetPackage: String? = null): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            targetPackage?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    internal fun startViewIntent(context: Context, intent: Intent): Boolean {
        val launchIntent = Intent(intent)
        if (context is Activity) {
            launchIntent.flags = launchIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
        }
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolveInfo != null
    }

    /** Try each intent in order; return true if any activity opened successfully. */
    fun startFirstResolvable(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            if (!isIntentResolvable(context, intent)) continue
            if (startViewIntent(context, intent)) return true
        }
        return false
    }
}
