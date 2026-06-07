package com.screenwakelock.detector.util

import android.os.Build

/** ROM / OEM hints for permission setup copy (not install trust). */
object DeviceOsHelper {

    fun isLineageOs(): Boolean =
        Build.DISPLAY.contains("lineage", ignoreCase = true) ||
            Build.FINGERPRINT.contains("lineage", ignoreCase = true) ||
            Build.BOARD.contains("lineage", ignoreCase = true)

    fun isOnePlusStockOs(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer in setOf("oneplus", "oppo", "realme") && !isLineageOs()
    }

}
