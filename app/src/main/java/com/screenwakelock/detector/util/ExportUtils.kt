package com.screenwakelock.detector.util

import android.content.Context
import android.content.Intent
import com.screenwakelock.detector.domain.model.WakeEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun toCsv(events: List<WakeEvent>): String {
        val header = listOf(
            "timestamp",
            "app",
            "package",
            "channel",
            "reason",
            "confidence",
            "root_enhanced",
            "wakelock_tag",
        ).joinToString(",")
        val rows = events.map { event ->
            listOf(
                csvDateFormat.format(Date(event.timestampMillis)),
                csvEscape(event.attributedAppLabel ?: ""),
                csvEscape(event.attributedPackage ?: ""),
                csvEscape(event.channelName ?: event.channelId ?: ""),
                event.reasonCode.name,
                "%.2f".format(Locale.US, event.confidence),
                event.rootEnhanced.toString(),
                csvEscape(event.wakelockTag ?: ""),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun shareCsv(context: Context, events: List<WakeEvent>) {
        val csv = toCsv(events)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Screen wake events export")
            putExtra(Intent.EXTRA_TEXT, csv)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Export wake history").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
