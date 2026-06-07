package com.screenwakelock.detector.util

import android.content.Context
import android.content.Intent
import com.screenwakelock.detector.domain.model.WakeEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    enum class ExportFormat { CSV, JSON }

    enum class DateRangePreset(val label: String, val days: Int?) {
        ALL("All time", null),
        LAST_7("Last 7 days", 7),
        LAST_30("Last 30 days", 30),
    }

    fun filterByDateRange(
        events: List<WakeEvent>,
        startMillis: Long?,
        endMillis: Long?,
    ): List<WakeEvent> {
        if (startMillis == null && endMillis == null) return events
        return events.filter { event ->
            val afterStart = startMillis == null || event.timestampMillis >= startMillis
            val beforeEnd = endMillis == null || event.timestampMillis <= endMillis
            afterStart && beforeEnd
        }
    }

    fun presetRangeMillis(preset: DateRangePreset): Pair<Long?, Long?> {
        val days = preset.days ?: return null to null
        val end = System.currentTimeMillis()
        val start = end - days.toLong() * 86_400_000L
        return start to end
    }

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
            "screen_off_duration_ms",
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
                event.screenOffDurationMs?.toString() ?: "",
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun toJson(events: List<WakeEvent>): String {
        val items = events.joinToString(",\n") { event -> eventToJsonObject(event) }
        return "[\n$items\n]"
    }

    fun share(
        context: Context,
        events: List<WakeEvent>,
        format: ExportFormat,
    ) {
        val content = when (format) {
            ExportFormat.CSV -> toCsv(events)
            ExportFormat.JSON -> toJson(events)
        }
        val mimeType = when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.JSON -> "application/json"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "Screen wake events export")
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Export wake history").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun shareCsv(context: Context, events: List<WakeEvent>) {
        share(context, events, ExportFormat.CSV)
    }

    private fun eventToJsonObject(event: WakeEvent): String {
        val fields = buildList {
            add("\"id\":${event.id}")
            add("\"timestampMillis\":${event.timestampMillis}")
            add(jsonStringField("attributedPackage", event.attributedPackage))
            add(jsonStringField("attributedAppLabel", event.attributedAppLabel))
            add(jsonStringField("channelId", event.channelId))
            add(jsonStringField("channelName", event.channelName))
            add("\"reasonCode\":\"${event.reasonCode.name}\"")
            add("\"confidence\":${"%.4f".format(Locale.US, event.confidence)}")
            add("\"rootEnhanced\":${event.rootEnhanced}")
            add(jsonStringField("wakelockTag", event.wakelockTag))
            add(jsonStringField("wakelockName", event.wakelockName))
            event.screenOffDurationMs?.let { add("\"screenOffDurationMs\":$it") }
        }
        return "  {${fields.joinToString(", ")}}"
    }

    private fun jsonStringField(key: String, value: String?): String =
        if (value == null) "\"$key\":null" else "\"$key\":\"${jsonEscape(value)}\""

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
