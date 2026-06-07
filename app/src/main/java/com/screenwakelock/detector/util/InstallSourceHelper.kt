package com.screenwakelock.detector.util

import android.content.Context
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Detects whether this app was installed outside trusted store/catalogue installers.
 *
 * GitHub APKs opened from a browser use [com.android.packageinstaller] as installer but are
 * still subject to Android 13+ restricted-settings policy (originating package = browser/files).
 */
object InstallSourceHelper {

    private const val PACKAGE_SOURCE_UNSPECIFIED = 0
    private const val PACKAGE_SOURCE_STORE = 1
    private const val PACKAGE_SOURCE_LOCAL_FILE = 2
    private const val PACKAGE_SOURCE_DOWNLOADED_FILE = 3

    /** Play Store, F-Droid, etc. — not subject to sideload restricted-settings blocks. */
    internal val TRUSTED_STORE_INSTALLERS = setOf(
        "com.android.vending",
        "org.fdroid.fdroid",
        "com.aurora.store",
    )

    /** System UI that installs APK files — trusted only when [InstallSourceSnapshot] says store source. */
    internal val PACKAGE_INSTALLER_PACKAGES = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )

    /** Delivered the APK bytes (browser download, Files, ADB). */
    internal val UNTRUSTED_ORIGIN_PACKAGES = setOf(
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.vivaldi.browser",
        "com.android.shell",
        "com.android.documentsui",
        "com.google.android.documentsui",
        "com.android.downloadui",
        "com.google.android.apps.docs",
        "com.coloros.filemanager",
        "com.oneplus.filemanager",
        "com.github.android",
    )

    data class InstallSourceSnapshot(
        val installingPackageName: String?,
        val initiatingPackageName: String?,
        val originatingPackageName: String?,
        val packageSource: Int = PACKAGE_SOURCE_UNSPECIFIED,
    )

    /** @deprecated Use [isTrustedStoreInstaller] — kept for unit tests. */
    internal fun isTrustedInstaller(installer: String?): Boolean = isTrustedStoreInstaller(installer)

    internal fun isTrustedStoreInstaller(installer: String?): Boolean =
        !installer.isNullOrBlank() && installer in TRUSTED_STORE_INSTALLERS

    internal fun isPackageInstallerPackage(pkg: String?): Boolean =
        !pkg.isNullOrBlank() && pkg in PACKAGE_INSTALLER_PACKAGES

    fun installingPackageName(context: Context): String? =
        installSourceSnapshot(context)?.let { it.installingPackageName ?: it.initiatingPackageName }

    fun installSourceSnapshot(context: Context): InstallSourceSnapshot? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val info = context.packageManager.getInstallSourceInfo(context.packageName)
            snapshotFrom(info)
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun snapshotFrom(info: InstallSourceInfo): InstallSourceSnapshot {
        val packageSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info.packageSource
        } else {
            PACKAGE_SOURCE_UNSPECIFIED
        }
        return InstallSourceSnapshot(
            installingPackageName = info.installingPackageName,
            initiatingPackageName = info.initiatingPackageName,
            originatingPackageName = info.originatingPackageName,
            packageSource = packageSource,
        )
    }

    /** True when install is not from a trusted store (GitHub download, Files, ADB, etc.). */
    fun isSideloaded(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val snapshot = installSourceSnapshot(context) ?: return true
        return isSideloadedInstall(snapshot)
    }

    internal fun isSideloadedInstall(snapshot: InstallSourceSnapshot): Boolean {
        val installer = snapshot.installingPackageName
        val initiating = snapshot.initiatingPackageName
        val originating = snapshot.originatingPackageName?.lowercase()

        if (isTrustedStoreInstaller(installer) || isTrustedStoreInstaller(initiating)) {
            return false
        }

        when (snapshot.packageSource) {
            PACKAGE_SOURCE_STORE -> return false
            PACKAGE_SOURCE_DOWNLOADED_FILE,
            PACKAGE_SOURCE_LOCAL_FILE,
            -> return true
        }

        val effectiveInstaller = installer ?: initiating

        if (isPackageInstallerPackage(effectiveInstaller)) {
            if (originating.isNullOrBlank()) return true
            if (isPackageInstallerPackage(originating)) return true
            if (originating in UNTRUSTED_ORIGIN_PACKAGES) return true
            if (!isTrustedStoreInstaller(originating)) return true
            return false
        }

        if (effectiveInstaller.isNullOrBlank()) return true
        if (effectiveInstaller == "com.android.shell") return true

        return !isTrustedStoreInstaller(effectiveInstaller)
    }
}
