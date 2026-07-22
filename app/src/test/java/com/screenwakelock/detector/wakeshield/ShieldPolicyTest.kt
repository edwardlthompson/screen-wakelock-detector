package com.screenwakelock.detector.wakeshield

import com.screenwakelock.detector.domain.model.ReasonCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldPolicyTest {

    private val self = "com.screenwakelock.detector"
    private val hard = ShieldExemptPackages.STATIC_HARD_EXEMPT + self

    private fun base(
        shieldEnabled: Boolean = true,
        inCooldown: Boolean = false,
        interactiveAbort: Boolean = false,
        packageName: String? = "com.spam.app",
        reasonCode: ReasonCode = ReasonCode.NOTIFICATION_HEADS_UP,
        hasFullScreenIntent: Boolean = false,
        fsiPermissionGranted: Boolean = false,
        userAllowlist: Set<String> = emptySet(),
    ) = ShieldPolicyInput(
        shieldEnabled = shieldEnabled,
        inCooldown = inCooldown,
        interactiveAbort = interactiveAbort,
        packageName = packageName,
        reasonCode = reasonCode,
        hasFullScreenIntent = hasFullScreenIntent,
        fsiPermissionGranted = fsiPermissionGranted,
        userAllowlist = userAllowlist,
        hardExempt = hard,
        selfPackage = self,
    )

    @Test
    fun disabled_skips() {
        assertEquals(ShieldDecision.SkipDisabled, ShieldPolicy.decide(base(shieldEnabled = false)))
    }

    @Test
    fun cooldown_skips() {
        assertEquals(ShieldDecision.SkipCooldown, ShieldPolicy.decide(base(inCooldown = true)))
    }

    @Test
    fun interactive_aborts() {
        assertEquals(
            ShieldDecision.AbortInteractive,
            ShieldPolicy.decide(base(interactiveAbort = true)),
        )
    }

    @Test
    fun dialer_exempt() {
        val dialer = "com.android.deskclock"
        val decision = ShieldPolicy.decide(base(packageName = dialer))
        assertEquals(ShieldDecision.AllowExempt, decision)
    }

    @Test
    fun emergency_exempt() {
        assertEquals(
            ShieldDecision.AllowExempt,
            ShieldPolicy.decide(base(packageName = "com.android.cellbroadcastreceiver")),
        )
    }

    @Test
    fun fsi_allows() {
        assertEquals(
            ShieldDecision.AllowFsi,
            ShieldPolicy.decide(
                base(
                    packageName = "com.alarm.app",
                    hasFullScreenIntent = true,
                ),
            ),
        )
    }

    @Test
    fun unknown_after_grace_is_hostile_without_appop() {
        val decision = ShieldPolicy.decide(
            base(packageName = null, reasonCode = ReasonCode.UNKNOWN),
        )
        assertTrue(decision is ShieldDecision.Hostile)
        decision as ShieldDecision.Hostile
        assertFalse(decision.allowAppOpDeny)
    }

    @Test
    fun identified_hostile_allows_appop() {
        val decision = ShieldPolicy.decide(base(packageName = "com.spam.app"))
        assertTrue(decision is ShieldDecision.Hostile)
        decision as ShieldDecision.Hostile
        assertTrue(decision.allowAppOpDeny)
    }

    @Test
    fun multiple_candidates_no_appop() {
        val decision = ShieldPolicy.decide(
            base(
                packageName = "com.spam.app",
                reasonCode = ReasonCode.MULTIPLE_CANDIDATES,
            ),
        )
        assertTrue(decision is ShieldDecision.Hostile)
        decision as ShieldDecision.Hostile
        assertFalse(decision.allowAppOpDeny)
    }

    @Test
    fun user_allowlist_exempt() {
        assertEquals(
            ShieldDecision.AllowExempt,
            ShieldPolicy.decide(
                base(
                    packageName = "com.allowed.app",
                    userAllowlist = setOf("com.allowed.app"),
                ),
            ),
        )
    }
}
