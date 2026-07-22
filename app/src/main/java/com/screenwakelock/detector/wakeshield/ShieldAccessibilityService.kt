package com.screenwakelock.detector.wakeshield

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal Accessibility service used solely for [GLOBAL_ACTION_LOCK_SCREEN].
 * Does not retrieve window content.
 */
class ShieldAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: ShieldAccessibilityService? = null
            private set

        fun isConnected(): Boolean = instance != null
    }
}
