package com.screenwakelock.detector.widget

import com.screenwakelock.detector.wakeshield.ShieldOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetShieldStatusTest {

    @Test
    fun line_offWhenDisarmed() {
        assertEquals("Shield off", WidgetShieldStatus.line(false, ShieldOutcome.LOCKED.name))
    }

    @Test
    fun line_armedWithoutOutcome() {
        assertEquals("Shield armed", WidgetShieldStatus.line(true, null))
    }

    @Test
    fun line_includesFriendlyOutcome() {
        assertEquals(
            "Shield: Locked screen",
            WidgetShieldStatus.line(true, ShieldOutcome.LOCKED.name),
        )
    }
}
