package com.screenwakelock.detector.root

import java.io.File

enum class RootManagerKind {
    UNKNOWN,
    MAGISK,
    KERNELSU,
    APATCH,
}

data class RootAvailabilityState(
    val isRooted: Boolean,
    val managerKind: RootManagerKind,
    val diagnostics: String,
)

class RootAvailability(
    private val rootShellService: RootShellService,
) {
    suspend fun probe(): RootAvailabilityState {
        val rooted = rootShellService.preheat()
        val manager = detectManager()
        val diagnostics = buildString {
            append("Root available: $rooted")
            append("\nManager: ${manager.name}")
        }
        return RootAvailabilityState(
            isRooted = rooted,
            managerKind = manager,
            diagnostics = diagnostics,
        )
    }

    private fun detectManager(): RootManagerKind {
        return when {
            File("/data/adb/magisk").exists() -> RootManagerKind.MAGISK
            File("/data/adb/ksu").exists() -> RootManagerKind.KERNELSU
            File("/data/adb/ap").exists() -> RootManagerKind.APATCH
            else -> RootManagerKind.UNKNOWN
        }
    }
}
