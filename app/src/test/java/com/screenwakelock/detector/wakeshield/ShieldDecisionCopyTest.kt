package com.screenwakelock.detector.wakeshield

import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldDecisionCopyTest {

    @Test
    fun why_includesFriendlyReasonAndActivePackages() {
        val json = """["com.life360.android.safetymapd","com.huawei.health"]"""
        val text = ShieldDecisionCopy.why(ShieldOutcome.LOCKED, detail = "grace", evidenceJson = json)
        assertTrue(text.contains("Relocked"))
        assertTrue(text.contains("grace"))
        assertTrue(text.contains("com.life360.android.safetymapd"))
    }

    @Test
    fun why_skipsEvidencePrefixDetail() {
        val text = ShieldDecisionCopy.why(
            ShieldOutcome.ALLOWED_EXEMPT,
            detail = "evidence: active=com.app",
        )
        assertTrue(text.contains("allowlisted"))
        assertTrue(!text.contains("evidence:"))
    }
}
