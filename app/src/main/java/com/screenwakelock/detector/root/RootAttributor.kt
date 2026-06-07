package com.screenwakelock.detector.root

import android.util.Log
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.root.parser.DumpsysBatteryStatsParser
import com.screenwakelock.detector.root.parser.DumpsysPowerParser
import com.screenwakelock.detector.root.parser.WakeupSourcesParser

data class RootSnapshot(
    val wakelockTag: String?,
    val wakelockName: String?,
    val uid: Int?,
    val reasonCode: ReasonCode?,
    val packageName: String?,
    val parserId: String? = null,
)

class RootAttributor(
    private val rootCommandRunner: RootCommandRunner,
    private val rootAvailability: RootAvailability,
) {
    suspend fun captureSnapshot(
        uidToPackage: (Int) -> String?,
    ): RootSnapshot? {
        if (!rootAvailability.probe().isRooted) {
            Log.d(TAG, "Root snapshot skipped — not rooted")
            return null
        }

        val powerResult = rootCommandRunner.execute(RootCommandAllowlist.DUMPSYS_POWER)
        if (powerResult.success && powerResult.output != null) {
            val parsed = DumpsysPowerParser.parse(powerResult.output)
            val active = parsed.wakelocks.firstOrNull { it.isActive }
            if (active != null) {
                val pkg = active.uid?.let(uidToPackage)
                Log.i(TAG, "Root wakelock from dumpsys power tag=${active.tag} uid=${active.uid}")
                return RootSnapshot(
                    wakelockTag = active.tag,
                    wakelockName = active.name,
                    uid = active.uid,
                    reasonCode = ReasonCode.ROOT_WAKELOCK,
                    packageName = pkg,
                    parserId = PARSER_DUMPSYS_POWER,
                )
            }
        } else {
            Log.w(TAG, "dumpsys power failed: ${powerResult.error}")
        }

        val batteryResult = rootCommandRunner.execute(RootCommandAllowlist.DUMPSYS_BATTERYSTATS_CHECKIN)
        if (batteryResult.success && batteryResult.output != null) {
            val parsed = DumpsysBatteryStatsParser.parse(batteryResult.output)
            val top = parsed.wakeEntries.maxByOrNull { it.count }
            if (top != null) {
                Log.i(TAG, "Root wakelock from batterystats tag=${top.tag} uid=${top.uid}")
                return RootSnapshot(
                    wakelockTag = top.tag,
                    wakelockName = top.tag.substringBefore(':'),
                    uid = top.uid,
                    reasonCode = ReasonCode.ROOT_WAKEUP_SOURCE,
                    packageName = uidToPackage(top.uid),
                    parserId = PARSER_DUMPSYS_BATTERYSTATS,
                )
            }
        } else {
            Log.w(TAG, "dumpsys batterystats failed: ${batteryResult.error}")
        }

        val wakeupResult = rootCommandRunner.execute(RootCommandAllowlist.WAKEUP_SOURCES)
        if (wakeupResult.success && wakeupResult.output != null) {
            val parsed = WakeupSourcesParser.parse(wakeupResult.output)
            val top = parsed.sources.maxByOrNull { it.wakeupCount }
            if (top != null) {
                Log.i(TAG, "Root wakeup source name=${top.name} count=${top.wakeupCount}")
                return RootSnapshot(
                    wakelockTag = top.name,
                    wakelockName = top.name,
                    uid = null,
                    reasonCode = ReasonCode.ROOT_WAKEUP_SOURCE,
                    packageName = null,
                    parserId = PARSER_WAKEUP_SOURCES,
                )
            }
        } else {
            Log.w(TAG, "wakeup_sources failed: ${wakeupResult.error}")
        }

        return null
    }

    companion object {
        private const val TAG = "RootAttributor"
        const val PARSER_DUMPSYS_POWER = "dumpsys_power_v2"
        const val PARSER_DUMPSYS_BATTERYSTATS = "dumpsys_batterystats_v1"
        const val PARSER_WAKEUP_SOURCES = "wakeup_sources_v1"

        fun parserDisplayName(parserId: String?): String? = when (parserId) {
            PARSER_DUMPSYS_POWER -> "dumpsys power v2"
            PARSER_DUMPSYS_BATTERYSTATS -> "dumpsys batterystats v1"
            PARSER_WAKEUP_SOURCES -> "wakeup sources v1"
            else -> parserId
        }
    }
}
