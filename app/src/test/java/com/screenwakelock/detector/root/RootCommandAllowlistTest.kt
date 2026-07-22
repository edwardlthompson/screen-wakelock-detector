package com.screenwakelock.detector.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootCommandAllowlistTest {

    @Test
    fun allowlist_acceptsKnownCommands() {
        assertTrue(RootCommandAllowlist.isAllowed("dumpsys power"))
        assertTrue(RootCommandAllowlist.isAllowed("dumpsys batterystats --checkin"))
        assertTrue(RootCommandAllowlist.isAllowed("input keyevent KEYCODE_SLEEP"))
        assertTrue(RootCommandAllowlist.isAllowed("input keyevent KEYCODE_POWER"))
    }

    @Test
    fun allowlist_rejectsArbitraryCommands() {
        assertFalse(RootCommandAllowlist.isAllowed("rm -rf /"))
        assertFalse(RootCommandAllowlist.isAllowed("dumpsys package com.android"))
        assertFalse(RootCommandAllowlist.isAllowed("cmd appops set evil; rm -rf / TURN_SCREEN_ON ignore"))
        assertFalse(RootCommandAllowlist.isAllowed("echo hi; reboot > /sys/power/wake_unlock"))
        assertFalse(RootCommandAllowlist.isAllowed("cmd appops set com.foo TURN_SCREEN_ON ignore; id"))
    }

    @Test
    fun templates_accept_valid_package_and_tag() {
        val ignore = RootCommandAllowlist.appopsTurnScreenOnIgnore("com.spam.app")
        assertNotNull(ignore)
        assertTrue(RootCommandAllowlist.isAllowed(ignore!!))

        val allow = RootCommandAllowlist.appopsTurnScreenOnAllow("com.spam.app")
        assertNotNull(allow)
        assertTrue(RootCommandAllowlist.isAllowed(allow!!))

        val unlock = RootCommandAllowlist.wakeUnlock("com.spam.app:AlarmAlert")
        assertNotNull(unlock)
        assertTrue(RootCommandAllowlist.isAllowed(unlock!!))
    }

    @Test
    fun templates_reject_injection() {
        assertNull(RootCommandAllowlist.appopsTurnScreenOnIgnore("com.foo; id"))
        assertNull(RootCommandAllowlist.appopsTurnScreenOnIgnore("com.foo app"))
        assertNull(RootCommandAllowlist.wakeUnlock("tag; reboot"))
        assertNull(RootCommandAllowlist.wakeUnlock("tag with spaces"))
    }

    @Test
    fun enum_sleep_command_stable() {
        assertEquals(
            "input keyevent KEYCODE_SLEEP",
            RootCommandAllowlist.INPUT_KEYCODE_SLEEP.command,
        )
    }
}
