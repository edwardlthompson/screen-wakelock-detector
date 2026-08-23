package com.screenwakelock.detector.domain.attributor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakelockTagDictionaryTest {

    @Test
    fun mapsKnownHolders() {
        assertEquals("Life360", WakelockTagDictionary.labelFor("com.life360.android.safetymapd"))
        assertEquals("Huawei Health", WakelockTagDictionary.labelForTag("com.huawei.health:gps"))
        assertNull(WakelockTagDictionary.labelFor("com.unknown.app"))
    }
}
