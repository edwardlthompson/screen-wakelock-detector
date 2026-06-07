package com.screenwakelock.detector.util

import android.content.Context
import android.os.Build

/**
 * Detects whether this app was installed outside trusted store/session installers.
 * Used to show restricted-settings guidance on Android 13+ sideloads.
 */
object InstallSourceHelper {

    /** Installers that are not subject to sideload restricted-settings blocks. */
    internal val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "org.fdroid.fdroid",
        "com.aurora.store",
        "com.apkmirror.helper",
        "com.apkpure.aegon",
    )

    internal fun isTrustedInstaller(installer: String?): Boolean =
        !installer.isNullOrBlank() && installer in TRUSTED_INSTALLERS

    fun installingPackageName(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val info = context.packageManager.getInstallSourceInfo(context.packageName)
            info.installingPackageName ?: info.initiatingPackageName
        } catch (_: Exception) {
            null
        }
    }

    /** True when install source is unknown or not a trusted installer. */
    fun isSideloaded(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true
        }
        return !isTrustedInstaller(installingPackageName(context))
    }
}
