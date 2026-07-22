package com.screenwakelock.detector.wakeshield

import com.screenwakelock.detector.domain.model.ActiveNotificationSnapshot
import com.screenwakelock.detector.domain.model.WakeCandidate
import org.json.JSONArray
import org.json.JSONObject

data class ShieldEvidenceSnapshot(
    val activePackages: List<String>,
    val wakeCapableCount: Int,
    val candidatePackages: List<String>,
    val summary: String,
)

object ShieldEvidence {
    fun build(
        active: List<ActiveNotificationSnapshot>,
        candidates: List<WakeCandidate>,
        wakelockTag: String?,
    ): ShieldEvidenceSnapshot {
        val packages = active.map { it.packageName }.distinct()
        val wakeCapable = active.count {
            it.hasFullScreenIntent || it.hasTurnScreenOn || it.importance >= 4
        }
        val candidatePkgs = candidates.map { it.packageName }.distinct()
        val parts = buildList {
            if (packages.isNotEmpty()) {
                add("active=${packages.take(8).joinToString(",")}")
            }
            if (wakeCapable > 0) add("wakeCapable=$wakeCapable")
            if (candidatePkgs.isNotEmpty()) {
                add("candidates=${candidatePkgs.take(5).joinToString(",")}")
            }
            if (!wakelockTag.isNullOrBlank()) add("tag=$wakelockTag")
        }
        return ShieldEvidenceSnapshot(
            activePackages = packages,
            wakeCapableCount = wakeCapable,
            candidatePackages = candidatePkgs,
            summary = parts.joinToString("; ").ifBlank { "no-signals" },
        )
    }

    fun encodePackages(packages: List<String>): String? {
        if (packages.isEmpty()) return null
        return JSONArray(packages).toString()
    }

    fun decodePackages(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getString(i))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun mergeDetail(outcomeDetail: String?, evidenceSummary: String?): String? {
        val parts = listOfNotNull(
            outcomeDetail?.takeIf { it.isNotBlank() },
            evidenceSummary?.takeIf { it.isNotBlank() }?.let { "evidence: $it" },
        )
        return parts.joinToString(" | ").ifBlank { null }
    }

    fun encodeOutcomeMeta(cancelled: Int, tiers: List<String>): String =
        JSONObject()
            .put("cancelled", cancelled)
            .put("tiers", JSONArray(tiers))
            .toString()
}
