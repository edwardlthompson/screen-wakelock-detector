package com.screenwakelock.detector.util

import android.content.Context
import android.net.Uri
import com.screenwakelock.detector.domain.model.WakeEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupUtils {
    private const val BACKUP_VERSION = 1

    data class BackupSettings(
        val monitoringEnabled: Boolean,
        val rootEnabled: Boolean,
        val alertOnEveryWake: Boolean,
        val thresholdAlertsEnabled: Boolean,
        val thresholdCount: Int,
        val nighttimeStartHour: Int,
        val nighttimeEndHour: Int,
        val quietHoursEnabled: Boolean,
        val ignoredPackages: Set<String>,
        val retentionDays: Int,
        val minWakeDurationMs: Int,
        val monitorScheduleEnabled: Boolean,
        val monitorPauseStartHour: Int,
        val monitorPauseEndHour: Int,
        val nightlyBudgets: Map<String, Int>,
    )

    data class ImportPreview(
        val eventCount: Int,
        val settingsPresent: Boolean,
    )

    fun buildBackupJson(events: List<WakeEvent>, settings: BackupSettings): String {
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("events", JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject().apply {
                        put("timestampMillis", event.timestampMillis)
                        put("attributedPackage", event.attributedPackage)
                        put("attributedAppLabel", event.attributedAppLabel)
                        put("channelId", event.channelId)
                        put("channelName", event.channelName)
                        put("reasonCode", event.reasonCode.name)
                        put("confidence", event.confidence.toDouble())
                        put("rootEnhanced", event.rootEnhanced)
                        put("wakelockTag", event.wakelockTag)
                        put("wakelockName", event.wakelockName)
                        put("rootParserId", event.rootParserId)
                        put("screenOffDurationMs", event.screenOffDurationMs)
                    },
                )
            }
        })
        root.put(
            "settings",
            JSONObject().apply {
                put("monitoringEnabled", settings.monitoringEnabled)
                put("rootEnabled", settings.rootEnabled)
                put("alertOnEveryWake", settings.alertOnEveryWake)
                put("thresholdAlertsEnabled", settings.thresholdAlertsEnabled)
                put("thresholdCount", settings.thresholdCount)
                put("nighttimeStartHour", settings.nighttimeStartHour)
                put("nighttimeEndHour", settings.nighttimeEndHour)
                put("quietHoursEnabled", settings.quietHoursEnabled)
                put("ignoredPackages", JSONArray(settings.ignoredPackages.toList()))
                put("retentionDays", settings.retentionDays)
                put("minWakeDurationMs", settings.minWakeDurationMs)
                put("monitorScheduleEnabled", settings.monitorScheduleEnabled)
                put("monitorPauseStartHour", settings.monitorPauseStartHour)
                put("monitorPauseEndHour", settings.monitorPauseEndHour)
                put(
                    "nightlyBudgets",
                    JSONObject().apply {
                        settings.nightlyBudgets.forEach { (pkg, count) ->
                            put(pkg, count)
                        }
                    },
                )
            },
        )
        return root.toString(2)
    }

    fun readImportPreview(context: Context, uri: Uri): ImportPreview {
        val json = readUriText(context, uri)
        val root = JSONObject(json)
        val events = root.optJSONArray("events") ?: JSONArray()
        return ImportPreview(
            eventCount = events.length(),
            settingsPresent = root.has("settings"),
        )
    }

    fun parseEvents(json: JSONObject): List<WakeEvent> {
        val array = json.optJSONArray("events") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    WakeEvent(
                        timestampMillis = obj.getLong("timestampMillis"),
                        attributedPackage = obj.optString("attributedPackage").takeIf { it.isNotEmpty() },
                        attributedAppLabel = obj.optString("attributedAppLabel").takeIf { it.isNotEmpty() },
                        channelId = obj.optString("channelId").takeIf { it.isNotEmpty() },
                        channelName = obj.optString("channelName").takeIf { it.isNotEmpty() },
                        reasonCode = com.screenwakelock.detector.domain.model.ReasonCode.valueOf(
                            obj.getString("reasonCode"),
                        ),
                        confidence = obj.getDouble("confidence").toFloat(),
                        rootEnhanced = obj.optBoolean("rootEnhanced", false),
                        wakelockTag = obj.optString("wakelockTag").takeIf { it.isNotEmpty() },
                        wakelockName = obj.optString("wakelockName").takeIf { it.isNotEmpty() },
                        rootParserId = obj.optString("rootParserId").takeIf { it.isNotEmpty() },
                        screenOffDurationMs = obj.optLong("screenOffDurationMs").takeIf { obj.has("screenOffDurationMs") },
                    ),
                )
            }
        }
    }

    fun writeToUri(context: Context, uri: Uri, json: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Cannot open output stream for backup")
    }

    fun readUriText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BufferedReader(InputStreamReader(input)).readText()
        } ?: error("Cannot open input stream for backup")
    }
}
