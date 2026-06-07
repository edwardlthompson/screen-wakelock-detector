package com.screenwakelock.detector.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkParserTest {

    @Test
    fun parseQuickFixWakeId() {
        val params = parseDeepLinkString("screenwakelock://app/quickfix/42")
        assertEquals(42L, params.quickFixWakeId)
        assertNull(params.wakeId)
    }

    @Test
    fun parseLatestQuickFix() {
        val params = parseDeepLinkString("screenwakelock://wake/latest/actions")
        assertEquals(0L, params.quickFixWakeId)
    }

    @Test
    fun parseDetailWakeId() {
        val params = parseDeepLinkString("screenwakelock://app/detail/7")
        assertEquals(7L, params.wakeId)
        assertNull(params.quickFixWakeId)
    }

    @Test
    fun parseSettingsRoot() {
        val params = parseDeepLinkString("screenwakelock://settings/root")
        assertEquals("root", params.route)
    }
}
