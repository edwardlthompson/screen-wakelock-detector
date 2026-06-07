package com.screenwakelock.detector.root.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class DumpsysPowerParserTest {

    @Test
    fun parse_api34_fixture_findsPartialWakeLock() {
        val fixture = loadFixture("root/dumpsys_power_api34.txt")
        val result = DumpsysPowerParser.parse(fixture)

        assertEquals(1, result.wakelocks.size)
        val lock = result.wakelocks.first()
        assertEquals("com.example.app", lock.name)
        assertEquals("com.example.app:notification", lock.tag)
        assertEquals(10123, lock.uid)
        assertEquals(4567, lock.pid)
        assertNotNull(result.wakeReason)
        assertEquals("application", result.wakeReason)
    }

    private fun loadFixture(path: String): String {
        val url = javaClass.classLoader?.getResource(path)
            ?: error("Fixture not found: app/src/test/resources/$path")
        return File(url.toURI()).readText()
    }
}
