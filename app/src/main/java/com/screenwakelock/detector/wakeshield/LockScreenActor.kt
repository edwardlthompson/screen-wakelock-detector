package com.screenwakelock.detector.wakeshield

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log

/**
 * L2: lock via Accessibility global action. No window content scraping.
 */
object LockScreenActor {
    private const val TAG = "LockScreenActor"

    fun isAvailable(): Boolean = ShieldAccessibilityService.instance != null

    fun lockNow(): Boolean {
        val service = ShieldAccessibilityService.instance ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val ok = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "GLOBAL_ACTION_LOCK_SCREEN result=$ok")
                ok
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
