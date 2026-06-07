package com.screenwakelock.detector.util

import android.content.Context
import com.screenwakelock.detector.service.NotificationCaptureService

/**
 * Best-effort silence for third-party apps. Android does not allow changing another app's
 * channel importance programmatically — we dismiss active notifications via the listener
 * service and deep-link to system channel settings for a durable fix.
 *
 * See docs/NOTIFICATIONS.md § OEM mute limits.
 */
object ChannelMuter {

    data class MuteResult(
        val dismissedCount: Int,
        val channelId: String?,
    )

    fun silenceNotifications(packageName: String, channelId: String?): MuteResult {
        val dismissed = NotificationCaptureService.dismissNotifications(packageName, channelId)
        return MuteResult(
            dismissedCount = dismissed,
            channelId = channelId,
        )
    }

    fun openBestSettings(context: Context, packageName: String, channelId: String?) {
        val intent = if (channelId != null && IntentUtils.canOpenChannelSettings()) {
            IntentUtils.channelNotificationSettings(packageName, channelId)
        } else {
            IntentUtils.appNotificationSettings(packageName)
        }
        context.startActivity(intent)
    }
}
