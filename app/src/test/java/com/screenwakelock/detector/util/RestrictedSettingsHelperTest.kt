package com.screenwakelock.detector.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictedSettingsHelperTest {

    @Test
    fun computeEffective_notRequiredBelowApi33() {
        assertTrue(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = false,
                sideloaded = true,
                appOpsAllowed = false,
                notificationListenerEnabled = false,
                usageStatsGranted = false,
            ),
        )
    }

    @Test
    fun computeEffective_appOpsAllow() {
        assertTrue(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = true,
                sideloaded = true,
                appOpsAllowed = true,
                notificationListenerEnabled = false,
                usageStatsGranted = false,
            ),
        )
    }

    @Test
    fun computeEffective_inferredFromNotificationGrant_onePlusOemQuirk() {
        assertTrue(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = true,
                sideloaded = true,
                appOpsAllowed = false,
                notificationListenerEnabled = true,
                usageStatsGranted = false,
            ),
        )
    }

    @Test
    fun computeEffective_inferredFromUsageGrant() {
        assertTrue(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = true,
                sideloaded = true,
                appOpsAllowed = false,
                notificationListenerEnabled = false,
                usageStatsGranted = true,
            ),
        )
    }

    @Test
    fun computeEffective_blockedWhenSideloadAndNoSignals() {
        assertFalse(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = true,
                sideloaded = true,
                appOpsAllowed = false,
                notificationListenerEnabled = false,
                usageStatsGranted = false,
            ),
        )
    }

    @Test
    fun computeEffective_notSideloaded() {
        assertTrue(
            RestrictedSettingsHelper.computeEffectiveRestrictedAccess(
                apiAtLeast33 = true,
                sideloaded = false,
                appOpsAllowed = false,
                notificationListenerEnabled = false,
                usageStatsGranted = false,
            ),
        )
    }
}
