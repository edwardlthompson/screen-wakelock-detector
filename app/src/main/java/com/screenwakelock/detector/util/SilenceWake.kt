package com.screenwakelock.detector.util

import android.content.Context
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.domain.model.WakeEventIdentity

object SilenceWake {

    fun silence(event: WakeEvent): ChannelMuter.MuteResult {
        val pkg = WakeEventIdentity.effectivePackage(event)
            ?: return ChannelMuter.MuteResult(0, event.channelId)
        return ChannelMuter.silenceNotifications(pkg, event.channelId)
    }

    fun openSettings(context: Context, event: WakeEvent) {
        val pkg = WakeEventIdentity.effectivePackage(event) ?: return
        ChannelMuter.openBestSettings(context, pkg, event.channelId)
    }

    fun snackbarMessage(result: ChannelMuter.MuteResult, appName: String): String =
        when {
            result.dismissedCount > 0 ->
                "Dismissed ${result.dismissedCount} notification(s) from $appName"
            else ->
                "Open settings to silence $appName"
        }
}
