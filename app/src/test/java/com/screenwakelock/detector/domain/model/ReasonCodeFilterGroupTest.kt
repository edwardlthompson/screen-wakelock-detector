package com.screenwakelock.detector.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReasonCodeFilterGroupTest {

    @Test
    fun filterGroup_notificationCodes_mapToNotifications() {
        listOf(
            ReasonCode.NOTIFICATION_HEADS_UP,
            ReasonCode.NOTIFICATION_RING,
            ReasonCode.NOTIFICATION_FULL_SCREEN,
            ReasonCode.NOTIFICATION_UNKNOWN,
        ).forEach { code ->
            assertEquals(ReasonFilterGroup.NOTIFICATIONS, code.filterGroup())
        }
    }

    @Test
    fun filterGroup_usageCodes_mapToUsage() {
        listOf(
            ReasonCode.USAGE_STATS_FOREGROUND,
            ReasonCode.USAGE_STATS_RECENT,
        ).forEach { code ->
            assertEquals(ReasonFilterGroup.USAGE, code.filterGroup())
        }
    }

    @Test
    fun filterGroup_rootCodes_mapToRoot() {
        listOf(
            ReasonCode.ROOT_WAKELOCK,
            ReasonCode.ROOT_WAKEUP_SOURCE,
        ).forEach { code ->
            assertEquals(ReasonFilterGroup.ROOT, code.filterGroup())
        }
    }

    @Test
    fun filterGroup_unknownCodes_mapToUnknown() {
        listOf(
            ReasonCode.UNKNOWN,
            ReasonCode.MULTIPLE_CANDIDATES,
        ).forEach { code ->
            assertEquals(ReasonFilterGroup.UNKNOWN, code.filterGroup())
        }
    }
}
