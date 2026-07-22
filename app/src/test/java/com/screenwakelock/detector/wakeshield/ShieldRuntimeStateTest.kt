package com.screenwakelock.detector.wakeshield

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldRuntimeStateTest {

    @Test
    fun cooldown_and_self_wake_windows() {
        val state = ShieldRuntimeState()
        val now = 1_000_000L
        assertFalse(state.inCooldown(now))
        assertFalse(state.isSelfWakeWindow(now))
        state.markEnforcement(now)
        assertTrue(state.inCooldown(now + 100))
        assertTrue(state.isSelfWakeWindow(now + 100))
        assertFalse(state.inCooldown(now + ShieldPolicy.COOLDOWN_MS + 1))
        assertFalse(state.isSelfWakeWindow(now + ShieldPolicy.SELF_WAKE_SUPPRESS_MS + 1))
        state.clear()
        assertFalse(state.inCooldown(now + 100))
    }
}
