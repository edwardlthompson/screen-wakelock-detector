package com.screenwakelock.detector.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayRefreshTest {

    @Test
    fun picks_highest_refresh_at_same_resolution() {
        val current = DisplayRefresh.ModeSpec(1, 1440, 3168, 60f)
        val modes = listOf(
            DisplayRefresh.ModeSpec(1, 1440, 3168, 60f),
            DisplayRefresh.ModeSpec(2, 1440, 3168, 90f),
            DisplayRefresh.ModeSpec(3, 1440, 3168, 120f),
            DisplayRefresh.ModeSpec(4, 1080, 2376, 120f),
        )
        val best = DisplayRefresh.fastestSameResolution(current, modes)
        assertEquals(3, best?.modeId)
        assertEquals(120f, best?.refreshRate)
    }

    @Test
    fun ignores_other_resolutions() {
        val current = DisplayRefresh.ModeSpec(1, 1080, 2376, 60f)
        val modes = listOf(
            DisplayRefresh.ModeSpec(1, 1080, 2376, 60f),
            DisplayRefresh.ModeSpec(2, 1440, 3168, 120f),
        )
        val best = DisplayRefresh.fastestSameResolution(current, modes)
        assertEquals(1, best?.modeId)
        assertEquals(60f, best?.refreshRate)
    }

    @Test
    fun empty_modes_returns_null() {
        val current = DisplayRefresh.ModeSpec(1, 1440, 3168, 60f)
        assertNull(DisplayRefresh.fastestSameResolution(current, emptyList()))
    }
}
