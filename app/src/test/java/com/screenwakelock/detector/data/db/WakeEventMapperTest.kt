package com.screenwakelock.detector.data.db

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeEventMapperTest {

    @Test
    fun entityToDomain_mapsStoredFields() {
        val entity = WakeEventEntity(
            id = 42L,
            timestampMillis = 1_700_000_000_000L,
            attributedPackage = "com.example.app",
            attributedAppLabel = "Example",
            channelId = "alerts",
            channelName = "Alerts",
            reasonCode = ReasonCode.NOTIFICATION_HEADS_UP.name,
            confidence = 0.92f,
            candidatesJson = null,
            rootEnhanced = false,
            wakelockTag = null,
            wakelockName = null,
            screenOffDurationMs = 12_000L,
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals("com.example.app", domain.attributedPackage)
        assertEquals(ReasonCode.NOTIFICATION_HEADS_UP, domain.reasonCode)
        assertEquals(0.92f, domain.confidence, 0.001f)
        assertTrue(domain.candidates.isEmpty())
    }

    @Test
    fun domainToEntity_preservesCoreFields() {
        val event = WakeEvent(
            id = 7L,
            timestampMillis = 1_700_000_001_000L,
            attributedPackage = "com.test.app",
            attributedAppLabel = "Test",
            channelId = null,
            channelName = null,
            reasonCode = ReasonCode.UNKNOWN,
            confidence = 0.1f,
            candidates = emptyList(),
            screenOffDurationMs = null,
        )

        val entity = event.toEntity()

        assertEquals(7L, entity.id)
        assertEquals("com.test.app", entity.attributedPackage)
        assertEquals(ReasonCode.UNKNOWN.name, entity.reasonCode)
        assertEquals(null, entity.candidatesJson)
    }
}
