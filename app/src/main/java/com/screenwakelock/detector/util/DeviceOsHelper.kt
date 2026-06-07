package com.screenwakelock.detector.util

import android.os.Build

/** ROM / OEM hints for permission setup copy (not install trust). */
object DeviceOsHelper {

    fun isLineageOs(): Boolean {
        if (Build.DISPLAY.contains("lineage", ignoreCase = true)) return true
        if (Build.FINGERPRINT.contains("lineage", ignoreCase = true)) return true
        if (Build.BOARD.contains("lineage", ignoreCase = true)) return true
        return lineageVersionProperty()?.isNotBlank() == true
    }

    fun isOnePlusStockOs(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer in setOf("oneplus", "oppo", "realme") && !isLineageOs()
    }

    private fun lineageVersionProperty(): String? = try {
        @Suppress("PrivateApi")
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        get.invoke(null, "ro.lineage.version") as? String
    } catch (_: Exception) {
        null
    }
}
