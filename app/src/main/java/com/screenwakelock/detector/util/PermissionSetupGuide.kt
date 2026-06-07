package com.screenwakelock.detector.util

import android.content.Context
import com.screenwakelock.detector.domain.model.PermissionKind

/**
 * Opens the correct system settings screens for sideload restricted-settings unlock.
 * Android does not expose an intent for the "Allow restricted settings" menu item itself.
 */
object PermissionSetupGuide {

    const val GITHUB_RELEASE_URL =
        "https://github.com/edwardlthompson/screen-wakelock-detector/releases/latest"

    /** Step 1: trigger the restricted-setting block dialog (required before ⋮ menu appears). */
    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(IntentUtils.notificationListenerSettings())
    }

    /** Step 2: App info — user taps ⋮ → Allow restricted settings. */
    fun openAppInfo(context: Context) {
        context.startActivity(IntentUtils.appDetailsSettings(context.packageName))
    }

    fun openPermissionSettings(context: Context, kind: PermissionKind) {
        val intent = when (kind) {
            PermissionKind.NOTIFICATION_LISTENER -> IntentUtils.notificationListenerSettings()
            PermissionKind.USAGE_STATS -> IntentUtils.usageAccessSettings()
            PermissionKind.POST_NOTIFICATIONS -> IntentUtils.appNotificationSettings(context.packageName)
            PermissionKind.BATTERY_OPTIMIZATION -> IntentUtils.requestIgnoreBatteryOptimizations(context)
        }
        context.startActivity(intent)
    }

    fun openReleaseDownload(context: Context) {
        IntentUtils.viewUri(context, GITHUB_RELEASE_URL)
    }
}
