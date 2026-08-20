package com.screenwakelock.detector.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class ShieldDigestCopyTest {

    @Test
    fun body_listsShieldedAndAllowed() {
        assertEquals(
            "4 shielded · 2 allowed this week",
            ShieldDigestCopy.body(ShieldWeekCounts(shielded = 4, allowed = 2, other = 1)),
        )
    }
}
