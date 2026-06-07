package com.screenwakelock.detector.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.screenwakelock.detector.domain.model.PermissionKind

data class PermissionSettingsGuide(
    val kind: PermissionKind,
    val title: String,
    val steps: List<String>,
    val intents: List<Intent>,
    val showInstallerWorkaround: Boolean = false,
)

object SettingsGuideProvider {

    private const val APP_LABEL = "Screen Wakelock Detector"

    fun guideFor(context: Context, kind: PermissionKind): PermissionSettingsGuide {
        val packageName = context.packageName
        return when (kind) {
            PermissionKind.RESTRICTED_SETTINGS -> PermissionSettingsGuide(
                kind = kind,
                title = "Allow restricted settings",
                steps = restrictedSettingsSteps(context),
                intents = listOf(IntentUtils.appDetailsSettings(packageName)),
                showInstallerWorkaround = true,
            )
            PermissionKind.NOTIFICATION_LISTENER -> PermissionSettingsGuide(
                kind = kind,
                title = "Notification access",
                steps = notificationAccessSteps(),
                intents = listOf(
                    IntentUtils.notificationListenerSettings(),
                    IntentUtils.appDetailsSettings(packageName),
                ),
            )
            PermissionKind.USAGE_STATS -> PermissionSettingsGuide(
                kind = kind,
                title = "Usage access",
                steps = usageAccessSteps(),
                intents = listOf(
                    IntentUtils.usageAccessSettings(),
                    IntentUtils.appDetailsSettings(packageName),
                ),
            )
            PermissionKind.POST_NOTIFICATIONS -> PermissionSettingsGuide(
                kind = kind,
                title = "Alert notifications",
                steps = postNotificationsSteps(),
                intents = listOf(
                    IntentUtils.appNotificationSettings(packageName),
                    IntentUtils.appDetailsSettings(packageName),
                ),
            )
            PermissionKind.BATTERY_OPTIMIZATION -> PermissionSettingsGuide(
                kind = kind,
                title = "Battery unrestricted",
                steps = batterySteps(context),
                intents = batteryIntents(context),
            )
        }
    }

    private fun restrictedSettingsSteps(context: Context): List<String> {
        return when {
            DeviceOsHelper.isLineageOs() -> lineageRestrictedSettingsSteps()
            DeviceOsHelper.isOnePlusStockOs() -> onePlusRestrictedSettingsSteps()
            else -> defaultRestrictedSettingsSteps()
        }
    }

    private fun lineageRestrictedSettingsSteps(): List<String> = listOf(
        "Tap Grant on Notification access — if blocked, tap OK on the Restricted setting dialog.",
        "Open App info (button below) → menu (⋮) → Allow restricted settings → confirm PIN.",
        "Return to Settings → Apps → Special app access and enable Notification access and Usage access.",
        "Return here — chips turn green when each permission is on.",
    )

    private fun onePlusRestrictedSettingsSteps(): List<String> = listOf(
        "Tap Grant on Notification access (or Open Settings below) to open Notification access.",
        "When the Restricted setting dialog appears, tap Allow — OxygenOS often grants here.",
        "If the toggle stays blocked: App info → menu (⋮) → Allow restricted settings → confirm PIN.",
        "Return here — the chip turns green once Notification or Usage access is enabled.",
    )

    private fun defaultRestrictedSettingsSteps(): List<String> = listOf(
        "Tap Grant on Notification access first — accept the blocked-permission dialog if shown.",
        "Open App info (button below) → menu (⋮) → Allow restricted settings → confirm PIN.",
        "Return here — the chip turns green once Notification or Usage access is enabled.",
    )

    private fun notificationAccessSteps(): List<String> = listOf(
        "Open Settings → Apps → Special app access → Notification access (or tap Open Settings).",
        "Enable $APP_LABEL.",
        "Return here — status updates automatically.",
    )

    private fun usageAccessSteps(): List<String> = listOf(
        "Open Settings → Apps → Special app access → Usage access (or tap Open Settings).",
        "Enable $APP_LABEL.",
        "Return here — status updates automatically.",
    )

    private fun postNotificationsSteps(): List<String> = listOf(
        "Open this app's notification settings.",
        "Turn notifications On.",
        "Return here — status updates automatically.",
    )

    private fun batterySteps(context: Context): List<String> {
        val oemStep = oemBatteryManualStep(context)
        return buildList {
            add("Open battery settings for $APP_LABEL (tap Open Settings).")
            add(oemStep)
            add("Return here — status updates automatically.")
        }
    }

    private fun batteryIntents(context: Context): List<Intent> = buildList {
        add(IntentUtils.requestIgnoreBatteryOptimizations(context))
        add(IntentUtils.batteryOptimizationSettings())
        add(IntentUtils.appDetailsSettings(context.packageName))
    }

    private fun oemBatteryManualStep(context: Context): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") ->
                "Set Battery to Unrestricted and remove $APP_LABEL from Sleeping apps if listed."
            manufacturer in setOf("xiaomi", "redmi", "poco") ->
                "In Security, turn Autostart On; in Battery, set No restrictions for $APP_LABEL."
            manufacturer in setOf("oneplus", "oppo", "realme") ->
                "Set Battery optimization to Don't optimize or allow background activity."
            else ->
                "Set Battery to Unrestricted (Settings → Apps → $APP_LABEL → Battery)."
        }
    }
}
