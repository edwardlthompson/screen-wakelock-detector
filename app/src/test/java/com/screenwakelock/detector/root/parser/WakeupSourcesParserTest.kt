package com.screenwakelock.detector.root.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WakeupSourcesParserTest {

    @Test
    fun parse_api34_fixture_findsActiveSources() {
        val fixture = loadFixture("root/wakeup_sources_api34.txt")
        val result = WakeupSourcesParser.parse(fixture)

        assertTrue(result.sources.any { it.name == "wlan" && it.wakeupCount == 3 })
        assertTrue(result.sources.any { it.name == "alarmtimer.0.auto" && it.wakeupCount == 1 })
    }

    @Test
    fun parse_ignoresZeroActivitySources() {
        val fixture = loadFixture("root/wakeup_sources_api34.txt")
        val result = WakeupSourcesParser.parse(fixture)

        assertTrue(result.sources.none { it.name == "btn_power_key" })
    }

    private fun loadFixture(path: String): String {
        val url = javaClass.classLoader?.getResource(path)
            ?: error("Fixture not found: app/src/test/resources/$path")
        return File(url.toURI()).readText()
    }
}
