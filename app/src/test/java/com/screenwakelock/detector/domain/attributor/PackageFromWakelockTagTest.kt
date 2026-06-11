package com.screenwakelock.detector.domain.attributor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackageFromWakelockTagTest {

    @Test
    fun extractPackage_parsesStandardTag() {
        assertEquals(
            "com.example.app",
            PackageFromWakelockTag.extractPackage("com.example.app:notification"),
        )
    }

    @Test
    fun extractPackage_parsesAlarmTag() {
        assertEquals(
            "com.example.app",
            PackageFromWakelockTag.extractPackage("com.example.app:AlarmAlert"),
        )
    }

    @Test
    fun extractPackage_rejectsKernelSourceNames() {
        assertNull(PackageFromWakelockTag.extractPackage("wlan"))
        assertNull(PackageFromWakelockTag.extractPackage("btn_power_key"))
    }

    @Test
    fun extractPackage_rejectsBlankAndNoColonPrefix() {
        assertNull(PackageFromWakelockTag.extractPackage(null))
        assertNull(PackageFromWakelockTag.extractPackage(""))
        assertNull(PackageFromWakelockTag.extractPackage("NotA.Package:tag"))
    }
}
