package com.screenwakelock.detector.util

import android.os.Build
import android.util.Log
import android.view.Window

/**
 * Picks the highest-refresh [android.view.Display.Mode] that matches the current mode's resolution.
 */
object DisplayRefresh {
    private const val TAG = "DisplayRefresh"

    data class ModeSpec(
        val modeId: Int,
        val physicalWidth: Int,
        val physicalHeight: Int,
        val refreshRate: Float,
    )

    fun fastestSameResolution(current: ModeSpec, modes: List<ModeSpec>): ModeSpec? {
        val matching = modes.filter {
            it.physicalWidth == current.physicalWidth &&
                it.physicalHeight == current.physicalHeight
        }
        return matching.maxByOrNull { it.refreshRate }
    }

    fun preferFastestSameResolutionMode(window: Window) {
        val display = window.decorView.display ?: return
        val current = display.mode ?: return
        val best = fastestSameResolution(
            current = ModeSpec(
                modeId = current.modeId,
                physicalWidth = current.physicalWidth,
                physicalHeight = current.physicalHeight,
                refreshRate = current.refreshRate,
            ),
            modes = display.supportedModes.map {
                ModeSpec(
                    modeId = it.modeId,
                    physicalWidth = it.physicalWidth,
                    physicalHeight = it.physicalHeight,
                    refreshRate = it.refreshRate,
                )
            },
        ) ?: return

        val attrs = window.attributes
        if (attrs.preferredDisplayModeId == best.modeId) {
            enableAdaptiveRefresh(window)
            return
        }
        attrs.preferredDisplayModeId = best.modeId
        window.attributes = attrs
        enableAdaptiveRefresh(window)
        Log.i(
            TAG,
            "preferredDisplayModeId=${best.modeId} " +
                "${best.physicalWidth}x${best.physicalHeight}@${best.refreshRate}Hz",
        )
    }

    /**
     * Keep ARR enabled so panels can still drop when idle and ramp on High votes.
     */
    private fun enableAdaptiveRefresh(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { window.setFrameRatePowerSavingsBalanced(true) }
        }
    }
}
