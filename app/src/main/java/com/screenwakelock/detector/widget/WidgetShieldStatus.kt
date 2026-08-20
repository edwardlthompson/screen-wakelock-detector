package com.screenwakelock.detector.widget

import com.screenwakelock.detector.wakeshield.ShieldOutcome

object WidgetShieldStatus {
    fun line(armed: Boolean, lastOutcome: String?): String {
        if (!armed) return "Shield off"
        val outcome = ShieldOutcome.fromStorage(lastOutcome)
        if (outcome == ShieldOutcome.NONE) return "Shield armed"
        return "Shield: ${outcome.friendlyLabel()}"
    }
}
