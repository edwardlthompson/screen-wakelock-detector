package com.screenwakelock.detector.domain.model


import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeEventIdentityTest {

    private fun event(
        attributedPackage: String? = null,
        wakelockTag: String? = null,
    ) = WakeEvent(
        timestampMillis = 1L,
        attributedPackage = attributedPackage,
        attributedAppLabel = null,
        channelId = null,
        channelName = null,
        reasonCode = ReasonCode.UNKNOWN,
        confidence = 0.5f,
        wakelockTag = wakelockTag,
    )

    @Test
    fun effectivePackage_prefersAttributedPackage() {
        assertEquals(
            "com.attributed.app",
            WakeEventIdentity.effectivePackage(
                event(attributedPackage = "com.attributed.app", wakelockTag = "com.tag.app:foo"),
            ),
        )
    }

    @Test
    fun effectivePackage_fallsBackToWakelockTag() {
        assertEquals(
            "com.example.app",
            WakeEventIdentity.effectivePackage(
                event(wakelockTag = "com.example.app:notification"),
            ),
        )
    }

    @Test
    fun effectivePackage_nullWhenNoIdentity() {
        assertNull(WakeEventIdentity.effectivePackage(event()))
    }

    @Test
    fun isIgnored_trueForEffectivePackage() {
        assertTrue(
            WakeEventIdentity.isIgnored(
                event(wakelockTag = "com.ignored.app:alarm"),
                setOf("com.ignored.app"),
            ),
        )
    }

    @Test
    fun isIgnored_falseWhenNoEffectivePackage() {
        assertFalse(WakeEventIdentity.isIgnored(event(), setOf("com.any.app")))
    }

    @Test
    fun isIgnored_nightOnlyDuringNightWindow() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 2)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val night = event(attributedPackage = "com.night.app").copy(timestampMillis = cal.timeInMillis)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 14)
        val day = night.copy(timestampMillis = cal.timeInMillis)
        val policy = IgnorePolicy(nightOnly = setOf("com.night.app"))
        assertTrue(WakeEventIdentity.isIgnored(night, policy))
        assertFalse(WakeEventIdentity.isIgnored(day, policy))
    }
}
