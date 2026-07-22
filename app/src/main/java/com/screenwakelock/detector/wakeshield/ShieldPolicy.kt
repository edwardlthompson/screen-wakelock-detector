package com.screenwakelock.detector.wakeshield

import com.screenwakelock.detector.domain.model.ReasonCode

data class ShieldPolicyInput(
    val shieldEnabled: Boolean,
    val inCooldown: Boolean,
    val interactiveAbort: Boolean,
    val packageName: String?,
    val reasonCode: ReasonCode,
    val hasFullScreenIntent: Boolean,
    val fsiPermissionGranted: Boolean,
    val userAllowlist: Set<String>,
    val hardExempt: Set<String>,
    val selfPackage: String,
)

sealed class ShieldDecision {
    data object SkipDisabled : ShieldDecision()
    data object SkipCooldown : ShieldDecision()
    data object AbortInteractive : ShieldDecision()
    data object AllowExempt : ShieldDecision()
    data object AllowFsi : ShieldDecision()
    data class Hostile(
        val packageName: String?,
        /** Sticky appops deny only when package is positively identified and not multi-candidate. */
        val allowAppOpDeny: Boolean,
    ) : ShieldDecision()
}

object ShieldPolicy {
    const val GRACE_MS = 1_500L
    const val COOLDOWN_MS = 5_000L
    const val SELF_WAKE_SUPPRESS_MS = 2_000L

    fun decide(input: ShieldPolicyInput): ShieldDecision {
        if (!input.shieldEnabled) return ShieldDecision.SkipDisabled
        if (input.inCooldown) return ShieldDecision.SkipCooldown
        if (input.interactiveAbort) return ShieldDecision.AbortInteractive

        val pkg = input.packageName
        if (pkg == input.selfPackage) return ShieldDecision.AllowExempt
        if (pkg != null && pkg in input.hardExempt) return ShieldDecision.AllowExempt
        if (pkg != null && pkg in input.userAllowlist) return ShieldDecision.AllowExempt

        val fsiExempt = input.hasFullScreenIntent ||
            input.reasonCode == ReasonCode.NOTIFICATION_FULL_SCREEN ||
            input.fsiPermissionGranted
        if (fsiExempt && pkg != null) return ShieldDecision.AllowFsi

        val unknown = pkg.isNullOrBlank() ||
            input.reasonCode == ReasonCode.UNKNOWN
        val multiple = input.reasonCode == ReasonCode.MULTIPLE_CANDIDATES
        // When !unknown, pkg is non-null/non-blank.
        val allowAppOpDeny = !unknown && !multiple &&
            pkg !in input.hardExempt &&
            pkg !in input.userAllowlist

        return ShieldDecision.Hostile(
            packageName = pkg,
            allowAppOpDeny = allowAppOpDeny,
        )
    }
}
