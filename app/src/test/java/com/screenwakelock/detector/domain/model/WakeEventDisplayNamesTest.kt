package com.screenwakelock.detector.domain.model

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeEventDisplayNamesTest {

    private fun event(
        attributedAppLabel: String? = null,
        attributedPackage: String? = null,
        wakelockTag: String? = null,
        wakelockName: String? = null,
        reasonCode: ReasonCode = ReasonCode.NOTIFICATION_UNKNOWN,
    ) = WakeEvent(
        timestampMillis = 1L,
        attributedPackage = attributedPackage,
        attributedAppLabel = attributedAppLabel,
        channelId = null,
        channelName = null,
        reasonCode = reasonCode,
        confidence = 0.8f,
        wakelockTag = wakelockTag,
        wakelockName = wakelockName,
    )

    @Test
    fun offlineAppName_prefersAttributedAppLabel() {
        assertEquals(
            "Example App",
            WakeEventDisplayNames.offlineAppName(
                event(attributedAppLabel = "Example App", attributedPackage = "com.example"),
            ),
        )
    }

    @Test
    fun offlineAppName_usesPackageWhenNoLabel() {
        assertEquals(
            "com.example.app",
            WakeEventDisplayNames.offlineAppName(event(attributedPackage = "com.example.app")),
        )
    }

    @Test
    fun offlineAppName_usesWakelockTagPackageWhenNoAttributedPackage() {
        assertEquals(
            "com.tagonly.app",
            WakeEventDisplayNames.offlineAppName(
                event(wakelockTag = "com.tagonly.app:notification", reasonCode = ReasonCode.ROOT_WAKELOCK),
            ),
        )
    }

    @Test
    fun offlineAppName_fallsBackToRawTagThenUnknown() {
        assertEquals(
            "raw:tag",
            WakeEventDisplayNames.offlineAppName(event(wakelockTag = "raw:tag")),
        )
        assertEquals(
            WakeEventDisplayNames.UNKNOWN,
            WakeEventDisplayNames.offlineAppName(event()),
        )
    }

    @Test
    fun displayAppName_delegatesToOfflineAppName() {
        val wake = event(wakelockTag = "com.tagonly.app:alarm", reasonCode = ReasonCode.ROOT_WAKELOCK)
        assertEquals(WakeEventDisplayNames.offlineAppName(wake), wake.displayAppName)
    }
}
