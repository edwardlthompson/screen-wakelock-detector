package com.screenwakelock.detector.root.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DumpsysBatteryStatsParserTest {

    @Test
    fun parse_api34_fixture_findsWakelockEntry() {
        val fixture = loadFixture("root/dumpsys_batterystats_api34.txt")
        val result = DumpsysBatteryStatsParser.parse(fixture)

        assertEquals(1, result.wakeEntries.size)
        assertEquals(10123, result.wakeEntries.first().uid)
        assertEquals("com.example.app:sync", result.wakeEntries.first().tag)
        assertEquals(12, result.wakeEntries.first().count)
    }

    private fun loadFixture(path: String): String {
        val url = javaClass.classLoader?.getResource(path)
            ?: error("Fixture not found: app/src/test/resources/$path")
        return File(url.toURI()).readText()
    }
}
