package com.screenwakelock.detector.util

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentUtilsTest {

    @Test
    fun wakeDetailDeepLink_hasExpectedPath() {
        assertEquals("screenwakelock://app/detail/42", IntentUtils.wakeDetailDeepLinkString(42L))
    }

    @Test
    fun quickFixDeepLink_hasExpectedPath() {
        assertEquals("screenwakelock://app/quickfix/99", IntentUtils.quickFixDeepLinkString(99L))
    }

    @Test
    fun latestQuickFixDeepLink_hasExpectedPath() {
        assertEquals("screenwakelock://wake/latest/actions", IntentUtils.latestQuickFixDeepLinkString())
    }

    @Test
    fun permissionsDeepLink_withHighlight() {
        assertEquals(
            "screenwakelock://app/permissions?highlight=notification_access",
            IntentUtils.permissionsDeepLinkString("notification_access"),
        )
    }

    @Test
    fun permissionsDeepLink_withoutHighlight() {
        assertEquals("screenwakelock://app/permissions", IntentUtils.permissionsDeepLinkString())
    }
}
