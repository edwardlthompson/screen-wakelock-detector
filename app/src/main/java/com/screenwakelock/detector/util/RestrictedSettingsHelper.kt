package com.screenwakelock.detector.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

/**
 * Android 13+ blocks sensitive permissions for sideloaded apps until the user enables
 * "Allow restricted settings" on the app's App info screen.
 */
object RestrictedSettingsHelper {

    private const val OP_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"

    fun isRestrictedSettingsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = appOps.unsafeCheckOpNoThrow(
                OP_ACCESS_RESTRICTED_SETTINGS,
                context.applicationInfo.uid,
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: SecurityException) {
            false
        }
    }

    /** Sideloaded and restricted-settings not yet unlocked. */
    fun needsUnlock(context: Context): Boolean =
        InstallSourceHelper.isSideloaded(context) && !isRestrictedSettingsAllowed(context)
}
