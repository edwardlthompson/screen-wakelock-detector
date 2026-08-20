package com.screenwakelock.detector.ui.modifiers

import android.os.Build
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

/**
 * Votes [View.REQUESTED_FRAME_RATE_CATEGORY_HIGH] while this scroll surface is attached
 * so adaptive-refresh panels can ramp during flings (API 35+).
 */
fun Modifier.highRefreshScroll(): Modifier = composed {
    val view = LocalView.current
    DisposableEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT)
            }
        }
    }
    this
}
