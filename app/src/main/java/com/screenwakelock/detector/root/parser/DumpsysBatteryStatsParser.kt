package com.screenwakelock.detector.root.parser

data class BatteryStatsWakeEntry(
    val uid: Int,
    val tag: String,
    val count: Int,
)

data class DumpsysBatteryStatsParseResult(
    val wakeEntries: List<BatteryStatsWakeEntry>,
)

object DumpsysBatteryStatsParser {
    private val wakeLine = Regex("""^9,(\d+),.*?wakelock,.*?,(.+?),(\d+)""")

    fun parse(output: String): DumpsysBatteryStatsParseResult {
        val entries = output.lineSequence()
            .mapNotNull { line ->
                val match = wakeLine.find(line.trim()) ?: return@mapNotNull null
                BatteryStatsWakeEntry(
                    uid = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null,
                    tag = match.groupValues[2],
                    count = match.groupValues[3].toIntOrNull() ?: 0,
                )
            }
            .toList()
        return DumpsysBatteryStatsParseResult(entries)
    }
}
