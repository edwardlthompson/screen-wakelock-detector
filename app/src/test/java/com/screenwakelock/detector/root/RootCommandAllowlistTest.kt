package com.screenwakelock.detector.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootCommandAllowlistTest {

    @Test
    fun allowlist_acceptsKnownCommands() {
        assertTrue(RootCommandAllowlist.isAllowed("dumpsys power"))
        assertTrue(RootCommandAllowlist.isAllowed("dumpsys batterystats --checkin"))
    }

    @Test
    fun allowlist_rejectsArbitraryCommands() {
        assertFalse(RootCommandAllowlist.isAllowed("rm -rf /"))
        assertFalse(RootCommandAllowlist.isAllowed("dumpsys package com.android"))
    }
}
