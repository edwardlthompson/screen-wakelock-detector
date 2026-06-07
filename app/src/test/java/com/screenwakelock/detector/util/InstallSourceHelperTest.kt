package com.screenwakelock.detector.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourceHelperTest {

    @Test
    fun trustedInstallers_includePlayStoreAndFdroid() {
        assertTrue(InstallSourceHelper.isTrustedInstaller("com.android.vending"))
        assertTrue(InstallSourceHelper.isTrustedInstaller("org.fdroid.fdroid"))
        assertTrue(InstallSourceHelper.isTrustedInstaller("com.google.android.packageinstaller"))
    }

    @Test
    fun sideloaded_whenInstallerNullOrUnknown() {
        assertFalse(InstallSourceHelper.isTrustedInstaller(null))
        assertFalse(InstallSourceHelper.isTrustedInstaller(""))
        assertFalse(InstallSourceHelper.isTrustedInstaller("com.android.documentsui"))
    }
}
