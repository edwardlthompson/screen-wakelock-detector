package com.screenwakelock.detector.wakeshield

/**
 * Persisted result of Wake Shield evaluation / enforcement for a wake event.
 */
enum class ShieldOutcome {
    NONE,
    ALLOWED_EXEMPT,
    ALLOWED_FSI,
    ABORTED_INTERACTIVE,
    CANCELLED_NOTIFS,
    LOCKED,
    SLEPT,
    DENIED_APPOP,
    ROOT_FAILED,
    PARTIAL,
    SUPPRESSED_SELF,
    PANIC_DISABLED,
    WOULD_HAVE_BLOCKED,
    ;

    fun friendlyLabel(): String = when (this) {
        NONE -> "None"
        ALLOWED_EXEMPT -> "Allowed (exempt)"
        ALLOWED_FSI -> "Allowed (full-screen intent)"
        ABORTED_INTERACTIVE -> "Aborted (interactive)"
        CANCELLED_NOTIFS -> "Cancelled notifications"
        LOCKED -> "Locked screen"
        SLEPT -> "Slept display"
        DENIED_APPOP -> "Denied TURN_SCREEN_ON"
        ROOT_FAILED -> "Root action failed"
        PARTIAL -> "Partial enforcement"
        SUPPRESSED_SELF -> "Suppressed (self-wake)"
        PANIC_DISABLED -> "Panic disabled"
        WOULD_HAVE_BLOCKED -> "Would have blocked (preview)"
    }

    companion object {
        fun fromStorage(raw: String?): ShieldOutcome {
            if (raw.isNullOrBlank()) return NONE
            return runCatching { valueOf(raw) }.getOrDefault(NONE)
        }
    }
}
