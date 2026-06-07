package com.screenwakelock.detector.root.parser

data class WakeupSourceEntry(
    val name: String,
    val activeCount: Int,
    val wakeupCount: Int,
)

data class WakeupSourcesParseResult(
    val sources: List<WakeupSourceEntry>,
)

object WakeupSourcesParser {
    // name active_count event_count wakeup_count expire_count ...
    private val sourceLine = Regex(
        """^(\S+)\s+(\d+)\s+\d+\s+(\d+)\s+\d+""",
    )

    fun parse(output: String): WakeupSourcesParseResult {
        val sources = output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("name") }
            .mapNotNull { line ->
                val match = sourceLine.find(line) ?: return@mapNotNull null
                WakeupSourceEntry(
                    name = match.groupValues[1],
                    activeCount = match.groupValues[2].toIntOrNull() ?: 0,
                    wakeupCount = match.groupValues[3].toIntOrNull() ?: 0,
                )
            }
            .filter { it.wakeupCount > 0 || it.activeCount > 0 }
            .toList()
        return WakeupSourcesParseResult(sources = sources)
    }
}
