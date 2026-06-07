package com.screenwakelock.detector.root

enum class RootCommandAllowlist(val command: String) {
    DUMPSYS_POWER("dumpsys power"),
    DUMPSYS_BATTERYSTATS_CHECKIN("dumpsys batterystats --checkin"),
    WAKEUP_SOURCES("cat /sys/kernel/debug/wakeup_sources"),
    ;

    companion object {
        fun fromUserInput(input: String): RootCommandAllowlist? =
            entries.find { it.command == input }

        fun isAllowed(input: String): Boolean = fromUserInput(input) != null
    }
}
