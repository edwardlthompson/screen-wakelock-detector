package com.screenwakelock.detector.root.parser

data class WakelockEntry(
    val name: String,
    val tag: String?,
    val uid: Int?,
    val pid: Int?,
    val isActive: Boolean,
)

data class DumpsysPowerParseResult(
    val wakelocks: List<WakelockEntry>,
    val wakeReason: String?,
)

object DumpsysPowerParser {
    private val wakeLockLine = Regex(
        """Wake Locks:\s*(size=\d+)?""",
        RegexOption.IGNORE_CASE,
    )
    private val partialWakeLock = Regex(
        """(?:\*|\s)*PARTIAL_WAKE_LOCK\s+'([^']+)'\s*(?:\(uid=(\d+)(?:\s+pid=(\d+))?\)|(?:\(uid=(\d+)\))?(?:\s*pid=(\d+))?)?""",
        RegexOption.IGNORE_CASE,
    )
    private val fullWakeLock = Regex(
        """(?:\*|\s)*FULL_WAKE_LOCK\s+'([^']+)'\s*(?:\(uid=(\d+)(?:\s+pid=(\d+))?\)|(?:\(uid=(\d+)\))?(?:\s*pid=(\d+))?)?""",
        RegexOption.IGNORE_CASE,
    )
    private val screenWakeLock = Regex(
        """(?:\*|\s)*SCREEN_(?:BRIGHT|DIM)_WAKE_LOCK\s+'([^']+)'\s*(?:\(uid=(\d+)(?:\s+pid=(\d+))?\)|(?:\(uid=(\d+)\))?(?:\s*pid=(\d+))?)?""",
        RegexOption.IGNORE_CASE,
    )
    private val wakeReasonLine = Regex(
        """mWakeReason=([^\s]+)""",
    )

    fun parse(output: String): DumpsysPowerParseResult {
        val wakelocks = mutableListOf<WakelockEntry>()
        var inWakeLocksSection = false
        var wakeReason: String? = null

        output.lineSequence().forEach { line ->
            wakeReasonLine.find(line)?.let { match ->
                wakeReason = match.groupValues[1]
            }

            if (wakeLockLine.containsMatchIn(line) || line.trim() == "Wake Locks:") {
                inWakeLocksSection = true
                return@forEach
            }

            if (inWakeLocksSection && line.isBlank()) {
                inWakeLocksSection = false
                return@forEach
            }

            if (inWakeLocksSection) {
                parseWakeLockLine(line)?.let { wakelocks.add(it) }
            } else {
                parseWakeLockLine(line)?.let { wakelocks.add(it) }
            }
        }

        return DumpsysPowerParseResult(
            wakelocks = wakelocks.distinctBy { "${it.name}:${it.tag}" },
            wakeReason = wakeReason,
        )
    }

    private fun parseWakeLockLine(line: String): WakelockEntry? {
        val patterns = listOf(partialWakeLock, fullWakeLock, screenWakeLock)
        for (pattern in patterns) {
            val match = pattern.find(line.trim()) ?: continue
            val tag = match.groupValues[1]
            val uid = listOf(2, 4).firstNotNullOfOrNull { i ->
                match.groupValues.getOrNull(i)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            }
            val pid = listOf(3, 5).firstNotNullOfOrNull { i ->
                match.groupValues.getOrNull(i)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            }
            return WakelockEntry(
                name = tag.substringBefore(':'),
                tag = tag,
                uid = uid,
                pid = pid,
                isActive = true,
            )
        }
        return null
    }
}
