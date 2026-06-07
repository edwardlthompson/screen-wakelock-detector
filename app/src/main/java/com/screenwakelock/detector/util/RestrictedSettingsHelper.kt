package com.screenwakelock.detector.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

/**
 * Android 13+ blocks sensitive permissions for sideloaded apps until the user enables
 * "Allow restricted settings" on the app's App info screen.
 *
 * Some OEMs (OnePlus/OxygenOS) leave [ACCESS_RESTRICTED_SETTINGS] AppOps at `default` even
 * after the user allows via the system dialog — infer unlock when notification or usage access
 * is already granted.
 */
object RestrictedSettingsHelper {

    private const val OP_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"

    /**
     * @param notificationListenerEnabled pass live listener state from [PermissionStatusRepository]
     * @param usageStatsGranted pass live usage state from [PermissionStatusRepository]
     */
    fun isRestrictedSettingsAllowed(
        context: Context,
        notificationListenerEnabled: Boolean = false,
        usageStatsGranted: Boolean = false,
    ): Boolean = computeEffectiveRestrictedAccess(
        apiAtLeast33 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        sideloaded = InstallSourceHelper.isSideloaded(context),
        appOpsAllowed = isAppOpsRestrictedSettingsAllowed(context),
        notificationListenerEnabled = notificationListenerEnabled,
        usageStatsGranted = usageStatsGranted,
    )

    internal fun computeEffectiveRestrictedAccess(
        apiAtLeast33: Boolean,
        sideloaded: Boolean,
        appOpsAllowed: Boolean,
        notificationListenerEnabled: Boolean,
        usageStatsGranted: Boolean,
    ): Boolean {
        if (!apiAtLeast33) return true
        if (!sideloaded) return true
        if (appOpsAllowed) return true
        if (notificationListenerEnabled || usageStatsGranted) return true
        return false
    }

    fun isAppOpsRestrictedSettingsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val op = OP_ACCESS_RESTRICTED_SETTINGS
            val mode = appOps.unsafeCheckOpNoThrow(
                op,
                context.applicationInfo.uid,
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: SecurityException) {
            false
        }
    }

    /** Sideloaded and restricted-settings not yet unlocked. */
    fun needsUnlock(context: Context): Boolean {
        if (!InstallSourceHelper.isSideloaded(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return !isRestrictedSettingsAllowed(
            context,
            notificationListenerEnabled = SensitivePermissionProbe.isNotificationListenerEnabled(context),
            usageStatsGranted = SensitivePermissionProbe.isUsageStatsGranted(context),
        )
    }
}

/** Minimal probes for [RestrictedSettingsHelper.needsUnlock] without repository coupling. */
internal object SensitivePermissionProbe {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabled.contains(context.packageName, ignoreCase = true)
    }

    fun isUsageStatsGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
