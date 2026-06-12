package com.screenwakelock.detector.domain.attributor

import android.app.Application
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppDisplayResolverTest {

    private val resolver = AppDisplayResolver(Application())

    private fun event(
        attributedAppLabel: String? = null,
        attributedPackage: String? = null,
        wakelockTag: String? = null,
        wakelockName: String? = null,
        confidence: Float = 0.9f,
        reasonCode: ReasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
    ) = WakeEvent(
        timestampMillis = 1L,
        attributedPackage = attributedPackage,
        attributedAppLabel = attributedAppLabel,
        channelId = null,
        channelName = null,
        reasonCode = reasonCode,
        confidence = confidence,
        wakelockTag = wakelockTag,
        wakelockName = wakelockName,
    )

    @Test
    fun resolveAppName_prefersAttributedAppLabel() {
        assertEquals(
            "Example App",
            resolver.resolveAppName(event(attributedAppLabel = "Example App", attributedPackage = "com.example")),
        )
    }

    @Test
    fun resolveAppName_usesWakelockTagWhenNoLabel() {
        assertEquals(
            "com.example.app",
            resolver.resolveAppName(event(wakelockTag = "com.example.app:notification")),
        )
    }

    @Test
    fun resolveSubtitle_showsWakelockTagForUnknownAppLabel() {
        assertEquals(
            "Wakelock: com.example.app:alarm",
            resolver.resolveSubtitle(
                event(
                    attributedAppLabel = AppDisplayResolver.UNKNOWN_APP_LABEL,
                    wakelockTag = "com.example.app:alarm",
                ),
            ),
        )
    }

    @Test
    fun resolveSubtitle_showsTagForLowConfidenceWhenDifferentFromName() {
        assertEquals(
            "Wakelock: com.example.app:background",
            resolver.resolveSubtitle(
                event(
                    attributedAppLabel = "Example",
                    wakelockTag = "com.example.app:background",
                    confidence = 0.4f,
                ),
            ),
        )
    }

    @Test
    fun resolveSubtitle_nullWhenNotNeeded() {
        assertNull(
            resolver.resolveSubtitle(event(attributedAppLabel = "Example", confidence = 0.9f)),
        )
    }
}
