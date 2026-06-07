package com.screenwakelock.detector.root

import android.content.Context
import android.content.Intent
import com.screenwakelock.detector.BuildConfig
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.TimeUtils

object RootDiagnosticExporter {

    fun buildReport(
        probeState: RootAvailabilityState?,
        lastDiagnostics: String?,
        recentRootEvents: List<WakeEvent>,
    ): String = buildString {
        appendLine("Screen Wakelock Detector — Root diagnostic report")
        appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Generated: ${TimeUtils.formatDateTime(System.currentTimeMillis())}")
        appendLine()
        appendLine("=== Root availability ===")
        if (probeState != null) {
            appendLine("Rooted: ${probeState.isRooted}")
            appendLine("Diagnostics: ${probeState.diagnostics}")
        } else {
            appendLine("Root probe not run")
        }
        appendLine()
        appendLine("=== Last command ===")
        appendLine(lastDiagnostics ?: "No diagnostics run yet")
        appendLine()
        appendLine("=== Recent root-enhanced wakes (metadata only) ===")
        if (recentRootEvents.isEmpty()) {
            appendLine("No root-enhanced wake events recorded")
        } else {
            recentRootEvents.forEach { event ->
                appendLine("- ${TimeUtils.formatDateTime(event.timestampMillis)}")
                appendLine("  App: ${event.attributedPackage ?: "unknown"}")
                appendLine("  Parser: ${RootAttributor.parserDisplayName(event.rootParserId) ?: "n/a"}")
                appendLine("  Wakelock tag: ${event.wakelockTag ?: "n/a"}")
                appendLine("  Reason: ${event.reasonCode.name}")
            }
        }
        appendLine()
        appendLine("Note: No notification message bodies are included in this report.")
    }

    fun shareReport(context: Context, report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Root diagnostic report")
            putExtra(Intent.EXTRA_TEXT, report)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(
            Intent.createChooser(intent, "Share root diagnostic report").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
    }
}
