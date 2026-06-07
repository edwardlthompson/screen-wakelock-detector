package com.screenwakelock.detector.root

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
        val diagnostics = buildString {
            append("Root available: $rooted")
            append("\nManager: ${RootManagerKind.UNKNOWN.name}")
            if (!rooted) {
                append("\nRoot-only features stay disabled until su is granted in Magisk/KernelSU.")
            }
        }
        return RootAvailabilityState(
            isRooted = rooted,
            managerKind = RootManagerKind.UNKNOWN,
            diagnostics = diagnostics,
        )
    }
}
