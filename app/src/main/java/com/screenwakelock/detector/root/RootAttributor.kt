package com.screenwakelock.detector.root

import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.root.parser.DumpsysBatteryStatsParser
import com.screenwakelock.detector.root.parser.DumpsysPowerParser

data class RootSnapshot(
    val wakelockTag: String?,
    val wakelockName: String?,
    val uid: Int?,
    val reasonCode: ReasonCode?,
    val packageName: String?,
)

class RootAttributor(
    private val rootCommandRunner: RootCommandRunner,
    private val rootAvailability: RootAvailability,
) {
    suspend fun captureSnapshot(
        uidToPackage: (Int) -> String?,
    ): RootSnapshot? {
        if (!rootAvailability.probe().isRooted) return null

        val powerResult = rootCommandRunner.execute(RootCommandAllowlist.DUMPSYS_POWER)
        if (powerResult.success && powerResult.output != null) {
            val parsed = DumpsysPowerParser.parse(powerResult.output)
            val active = parsed.wakelocks.firstOrNull { it.isActive }
            if (active != null) {
                val pkg = active.uid?.let(uidToPackage)
                return RootSnapshot(
                    wakelockTag = active.tag,
                    wakelockName = active.name,
                    uid = active.uid,
                    reasonCode = ReasonCode.ROOT_WAKELOCK,
                    packageName = pkg,
                )
            }
        }

        val batteryResult = rootCommandRunner.execute(RootCommandAllowlist.DUMPSYS_BATTERYSTATS_CHECKIN)
        if (batteryResult.success && batteryResult.output != null) {
            val parsed = DumpsysBatteryStatsParser.parse(batteryResult.output)
            val top = parsed.wakeEntries.maxByOrNull { it.count }
            if (top != null) {
                return RootSnapshot(
                    wakelockTag = top.tag,
                    wakelockName = top.tag.substringBefore(':'),
                    uid = top.uid,
                    reasonCode = ReasonCode.ROOT_WAKEUP_SOURCE,
                    packageName = uidToPackage(top.uid),
                )
            }
        }

        return null
    }
}
