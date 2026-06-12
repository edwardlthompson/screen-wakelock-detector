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
    fun parseSettingsRootAutomation() {
        val params = parseDeepLinkString("screenwakelock://settings/root?automation=enable")
        assertEquals("root", params.route)
        assertEquals("enable", params.rootAutomation)
    }

    @Test
    fun parseSettingsDonateAutomation() {
        val params = parseDeepLinkString("screenwakelock://settings/donate?automation=open")
        assertEquals("settings", params.route)
        assertEquals("open", params.donateAutomation)
    }

    @Test
    fun parseSettingsRoute() {
        val params = parseDeepLinkString("screenwakelock://settings")
        assertEquals("settings", params.route)
    }

    @Test
    fun parseSettingsRoot() {
        val params = parseDeepLinkString("screenwakelock://settings/root")
        assertEquals("root", params.route)
    }

    @Test
    fun parseWakeLatest() {
        val params = parseDeepLinkString("screenwakelock://wake/latest")
        assertEquals(0L, params.quickFixWakeId)
    }

    @Test
    fun parseHistorySearchQuery() {
        val params = parseDeepLinkString("screenwakelock://history?q=search.test")
        assertEquals("history", params.route)
        assertEquals("search.test", params.historyQuery)
    }
}
