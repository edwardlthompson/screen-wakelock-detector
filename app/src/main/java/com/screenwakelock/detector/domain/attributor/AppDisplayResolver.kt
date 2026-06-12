package com.screenwakelock.detector.domain.attributor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.domain.model.WakeEventDisplayNames
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDisplayResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolveAppName(event: WakeEvent): String = resolveAppNameInternal(event)

    /**
     * Secondary line when the primary name is still ambiguous (unknown app or raw tag).
     */
    fun resolveSubtitle(event: WakeEvent): String? {
        val name = resolveAppNameInternal(event)
        if (name == UNKNOWN_APP_LABEL) {
            return event.wakelockTag?.let { "Wakelock: $it" }
                ?: event.wakelockName?.let { "Source: $it" }
        }
        if (event.isLowConfidence && event.wakelockTag != null && event.wakelockTag != name) {
            return "Wakelock: ${event.wakelockTag}"
        }
        return null
    }

    private fun resolveAppNameInternal(event: WakeEvent): String {
        event.attributedAppLabel?.let { return it }
        event.attributedPackage?.let { pkg ->
            return resolvePmLabel(pkg) ?: pkg
        }
        PackageFromWakelockTag.extractPackage(event.wakelockTag)?.let { pkg ->
            return resolvePmLabel(pkg) ?: pkg
        }
        return WakeEventDisplayNames.offlineAppName(event)
    }

    private fun resolvePmLabel(packageName: String): String? = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(info).toString()
    }.getOrNull()

    companion object {
        const val UNKNOWN_APP_LABEL = "Unknown app"

        fun resolveAppName(context: Context, event: WakeEvent): String =
            AppDisplayResolver(context).resolveAppNameInternal(event)
    }
}
