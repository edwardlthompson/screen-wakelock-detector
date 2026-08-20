package com.screenwakelock.detector.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUtilsTest {

    @Test
    fun backupSettings_holdsNightIgnoredPackages() {
        val settings = BackupUtils.BackupSettings(
            monitoringEnabled = true,
            rootEnabled = false,
            alertOnEveryWake = false,
            thresholdAlertsEnabled = true,
            thresholdCount = 3,
            nighttimeStartHour = 23,
            nighttimeEndHour = 6,
            quietHoursEnabled = false,
            ignoredPackages = setOf("com.always"),
            nightIgnoredPackages = setOf("com.night"),
            retentionDays = 0,
            minWakeDurationMs = 0,
            monitorScheduleEnabled = false,
            monitorPauseStartHour = 23,
            monitorPauseEndHour = 7,
            nightlyBudgets = emptyMap(),
        )
        assertEquals(setOf("com.night"), settings.nightIgnoredPackages)
        assertTrue("com.always" in settings.ignoredPackages)
    }
}
