package com.screenwakelock.detector.wakeshield

object ShieldDecisionCopy {
    fun why(
        outcome: ShieldOutcome,
        detail: String? = null,
        evidenceJson: String? = null,
    ): String {
        val reason = when (outcome) {
            ShieldOutcome.NONE -> "No shield decision for this wake."
            ShieldOutcome.ALLOWED_EXEMPT ->
                "Allowed after grace — allowlisted, emergency, or OEM-exempt app."
            ShieldOutcome.ALLOWED_FSI ->
                "Allowed — a full-screen intent was treated as a legitimate wake."
            ShieldOutcome.ABORTED_INTERACTIVE ->
                "Stopped — you were already using the phone."
            ShieldOutcome.CANCELLED_NOTIFS ->
                "Cancelled the notification that kept the screen on."
            ShieldOutcome.LOCKED ->
                "Relocked the screen after the grace window."
            ShieldOutcome.SLEPT ->
                "Sent the display to sleep (root)."
            ShieldOutcome.DENIED_APPOP ->
                "Denied TURN_SCREEN_ON for this app."
            ShieldOutcome.ROOT_FAILED ->
                "Root sleep or deny did not run — check Magisk/KSU grant."
            ShieldOutcome.PARTIAL ->
                "Some shield tiers ran; not every step completed."
            ShieldOutcome.SUPPRESSED_SELF ->
                "Ignored a wake caused by the shield itself."
            ShieldOutcome.PANIC_DISABLED ->
                "Shield was panic-disabled."
        }
        val packages = packagesFrom(evidenceJson).take(4)
        val evidence = if (packages.isEmpty()) {
            ""
        } else {
            " Active notifications: ${packages.joinToString()}."
        }
        val extra = detail
            ?.takeIf { it.isNotBlank() && !it.startsWith("evidence:") }
            ?.let { " $it" }
            ?: ""
        return reason + evidence + extra
    }

    internal fun packagesFrom(evidenceJson: String?): List<String> {
        val decoded = ShieldEvidence.decodePackages(evidenceJson)
        if (decoded.isNotEmpty()) return decoded
        if (evidenceJson.isNullOrBlank()) return emptyList()
        return Regex("\"([^\"]+)\"").findAll(evidenceJson).map { it.groupValues[1] }.toList()
    }
}
