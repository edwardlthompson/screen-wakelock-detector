package com.screenwakelock.detector.util

import android.content.Context
import com.screenwakelock.detector.domain.model.PermissionKind

sealed class SettingsOpenResult {
    data object Opened : SettingsOpenResult()

    data class ShowManualSteps(
        val guide: PermissionSettingsGuide,
    ) : SettingsOpenResult()
}

/**
 * Opens the correct system settings screens for permission setup.
 * Android does not expose an intent for the "Allow restricted settings" menu item itself.
 */
object PermissionSetupGuide {

    const val GITHUB_RELEASE_URL =
        "https://github.com/edwardlthompson/screen-wakelock-detector/releases/latest"

    fun openWithFallback(context: Context, kind: PermissionKind): SettingsOpenResult {
        if (RestrictedSettingsHelper.needsUnlock(context)) {
            when (kind) {
                PermissionKind.NOTIFICATION_LISTENER,
                PermissionKind.USAGE_STATS,
                PermissionKind.RESTRICTED_SETTINGS,
                -> {
                    openRestrictedSettingsTrigger(context)
                    return SettingsOpenResult.ShowManualSteps(
                        SettingsGuideProvider.guideFor(context, PermissionKind.RESTRICTED_SETTINGS),
                    )
                }
                else -> { /* fall through to normal open */ }
            }
        }

        if (kind == PermissionKind.RESTRICTED_SETTINGS) {
            openRestrictedSettingsTrigger(context)
            return SettingsOpenResult.ShowManualSteps(
                SettingsGuideProvider.guideFor(context, kind),
            )
        }

        val guide = SettingsGuideProvider.guideFor(context, kind)
        return if (IntentUtils.startFirstResolvable(context, guide.intents)) {
            SettingsOpenResult.Opened
        } else {
            SettingsOpenResult.ShowManualSteps(guide)
        }
    }

    /**
     * Opens this app's notification-access toggle when possible so the system shows the
     * restricted-settings block dialog on sideloaded installs.
     */
    fun openRestrictedSettingsTrigger(context: Context) {
        if (DeviceOsHelper.prefersAppInfoRestrictedUnlock()) {
            openAppInfo(context)
        } else {
            IntentUtils.startFirstResolvable(context, IntentUtils.restrictedSettingsTriggerIntents(context))
        }
    }

    /** @deprecated Use [openRestrictedSettingsTrigger] */
    fun openNotificationListenerSettings(context: Context) {
        openRestrictedSettingsTrigger(context)
    }

    fun openAppInfo(context: Context) {
        context.startActivity(IntentUtils.appDetailsSettings(context.packageName))
    }

    fun openPermissionSettings(context: Context, kind: PermissionKind) {
        when (val result = openWithFallback(context, kind)) {
            is SettingsOpenResult.Opened -> Unit
            is SettingsOpenResult.ShowManualSteps -> Unit
        }
    }

    fun openReleaseDownload(context: Context) {
        IntentUtils.viewUri(context, GITHUB_RELEASE_URL)
    }
}
