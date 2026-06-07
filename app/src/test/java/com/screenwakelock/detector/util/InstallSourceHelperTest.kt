package com.screenwakelock.detector.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourceHelperTest {

    private companion object {
        const val PACKAGE_SOURCE_STORE = 1
        const val PACKAGE_SOURCE_DOWNLOADED_FILE = 3
    }

    @Test
    fun trustedStoreInstallers_includePlayStoreAndFdroid() {
        assertTrue(InstallSourceHelper.isTrustedStoreInstaller("com.android.vending"))
        assertTrue(InstallSourceHelper.isTrustedStoreInstaller("org.fdroid.fdroid"))
    }

    @Test
    fun packageInstaller_isNotTrustedStore() {
        assertFalse(InstallSourceHelper.isTrustedStoreInstaller("com.android.packageinstaller"))
        assertFalse(InstallSourceHelper.isTrustedStoreInstaller("com.google.android.packageinstaller"))
    }

    @Test
    fun sideloaded_githubViaFirefox_packageInstallerOrigin() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "com.android.packageinstaller",
            initiatingPackageName = "com.android.packageinstaller",
            originatingPackageName = "org.mozilla.firefox",
        )
        assertTrue(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun sideloaded_adbShell_nullInstaller() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = null,
            initiatingPackageName = "com.android.shell",
            originatingPackageName = null,
        )
        assertTrue(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun notSideloaded_playStore() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "com.android.vending",
            initiatingPackageName = null,
            originatingPackageName = null,
        )
        assertFalse(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun notSideloaded_fdroid() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "org.fdroid.fdroid",
            initiatingPackageName = "org.fdroid.fdroid",
            originatingPackageName = null,
        )
        assertFalse(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun sideloaded_packageSourceDownloadedFile_api34() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "com.android.packageinstaller",
            initiatingPackageName = null,
            originatingPackageName = null,
            packageSource = PACKAGE_SOURCE_DOWNLOADED_FILE,
        )
        assertTrue(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun notSideloaded_packageSourceStore_api34() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "com.android.packageinstaller",
            initiatingPackageName = null,
            originatingPackageName = null,
            packageSource = PACKAGE_SOURCE_STORE,
        )
        assertFalse(InstallSourceHelper.isSideloadedInstall(snapshot))
    }

    @Test
    fun sideloaded_filesAppOrigin() {
        val snapshot = InstallSourceHelper.InstallSourceSnapshot(
            installingPackageName = "com.android.packageinstaller",
            initiatingPackageName = null,
            originatingPackageName = "com.google.android.documentsui",
        )
        assertTrue(InstallSourceHelper.isSideloadedInstall(snapshot))
    }
}
