package com.screenwakelock.detector.root.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DumpsysPowerParserTest {

    @Test
    fun parse_api34_fixture_findsPartialWakeLock() {
        assertPowerFixture("root/dumpsys_power_api34.txt", "com.example.app:notification")
    }

    @Test
    fun parse_api29_fixture_findsPartialWakeLock() {
        assertPowerFixture("root/dumpsys_power_api29.txt", "com.example.app:AlarmAlert")
    }

    @Test
    fun parse_api31_fixture_findsPartialWakeLock() {
        assertPowerFixture("root/dumpsys_power_api31.txt", "com.example.app:fg_service")
    }

    @Test
    fun parse_lineage16_fixture_findsKnownHolders() {
        val result = DumpsysPowerParser.parse(loadFixture("root/dumpsys_power_lineage16.txt"))
        assertEquals(2, result.wakelocks.size)
        assertEquals("com.life360.android.safetymapd:location", result.wakelocks[0].tag)
        assertEquals("com.huawei.health:wear", result.wakelocks[1].tag)
    }

    private fun assertPowerFixture(path: String, expectedTag: String) {
        val fixture = loadFixture(path)
        val result = DumpsysPowerParser.parse(fixture)
        assertEquals(1, result.wakelocks.size)
        assertEquals(expectedTag, result.wakelocks.first().tag)
        assertEquals(10123, result.wakelocks.first().uid)
    }

    private fun loadFixture(path: String): String {
        val url = javaClass.classLoader?.getResource(path)
            ?: error("Fixture not found: app/src/test/resources/$path")
        return File(url.toURI()).readText()
    }
}
