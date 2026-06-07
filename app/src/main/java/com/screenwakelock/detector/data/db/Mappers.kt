package com.screenwakelock.detector.data.db

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeCandidate
import com.screenwakelock.detector.domain.model.WakeEvent
import org.json.JSONArray
import org.json.JSONObject

fun WakeEventEntity.toDomain(): WakeEvent = WakeEvent(
    id = id,
    timestampMillis = timestampMillis,
    attributedPackage = attributedPackage,
    attributedAppLabel = attributedAppLabel,
    channelId = channelId,
    channelName = channelName,
    reasonCode = ReasonCode.valueOf(reasonCode),
    confidence = confidence,
    candidates = candidatesJson?.let { parseCandidates(it) } ?: emptyList(),
    rootEnhanced = rootEnhanced,
    wakelockTag = wakelockTag,
    wakelockName = wakelockName,
    screenOffDurationMs = screenOffDurationMs,
)

fun WakeEvent.toEntity(): WakeEventEntity = WakeEventEntity(
    id = id,
    timestampMillis = timestampMillis,
    attributedPackage = attributedPackage,
    attributedAppLabel = attributedAppLabel,
    channelId = channelId,
    channelName = channelName,
    reasonCode = reasonCode.name,
    confidence = confidence,
    candidatesJson = if (candidates.isEmpty()) null else encodeCandidates(candidates),
    rootEnhanced = rootEnhanced,
    wakelockTag = wakelockTag,
    wakelockName = wakelockName,
    screenOffDurationMs = screenOffDurationMs,
)

fun encodeCandidates(candidates: List<WakeCandidate>): String {
    val array = JSONArray()
    candidates.forEach { candidate ->
        array.put(
            JSONObject().apply {
                put("packageName", candidate.packageName)
                put("appLabel", candidate.appLabel)
                put("channelId", candidate.channelId)
                put("channelName", candidate.channelName)
                put("reasonCode", candidate.reasonCode.name)
                put("confidence", candidate.confidence.toDouble())
                put("detail", candidate.detail)
            },
        )
    }
    return array.toString()
}

fun parseCandidates(json: String): List<WakeCandidate> {
    val array = JSONArray(json)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
                WakeCandidate(
                    packageName = obj.getString("packageName"),
                    appLabel = obj.optString("appLabel").takeIf { it.isNotEmpty() },
                    channelId = obj.optString("channelId").takeIf { it.isNotEmpty() },
                    channelName = obj.optString("channelName").takeIf { it.isNotEmpty() },
                    reasonCode = ReasonCode.valueOf(obj.getString("reasonCode")),
                    confidence = obj.getDouble("confidence").toFloat(),
                    detail = obj.optString("detail").takeIf { it.isNotEmpty() },
                ),
            )
        }
    }
}

fun NotificationCacheEntity.toDomain() = com.screenwakelock.detector.domain.model.CachedNotification(
    id = id,
    packageName = packageName,
    channelId = channelId,
    channelName = channelName,
    postedAtMillis = postedAtMillis,
    category = category,
    importance = importance,
)
