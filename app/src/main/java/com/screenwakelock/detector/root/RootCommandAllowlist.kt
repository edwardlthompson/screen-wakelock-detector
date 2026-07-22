package com.screenwakelock.detector.root

/**
 * Fixed allowlist of root commands. Templated commands are validated by charset
 * before acceptance — never interpolate untrusted user input.
 */
enum class RootCommandAllowlist(val command: String) {
    DUMPSYS_POWER("dumpsys power"),
    DUMPSYS_BATTERYSTATS_CHECKIN("dumpsys batterystats --checkin"),
    WAKEUP_SOURCES("cat /sys/kernel/debug/wakeup_sources"),
    INPUT_KEYCODE_SLEEP("input keyevent KEYCODE_SLEEP"),
    INPUT_KEYCODE_POWER("input keyevent KEYCODE_POWER"),
    ;

    companion object {
        private val PACKAGE_REGEX = Regex("^[a-zA-Z0-9._]+$")
        private val TAG_REGEX = Regex("^[A-Za-z0-9_.:/-]{1,128}$")
        private val APPOPS_IGNORE =
            Regex("^cmd appops set ([a-zA-Z0-9._]+) TURN_SCREEN_ON ignore$")
        private val APPOPS_ALLOW =
            Regex("^cmd appops set ([a-zA-Z0-9._]+) TURN_SCREEN_ON (allow|default)$")
        private val WAKE_UNLOCK =
            Regex("^echo ([A-Za-z0-9_.:/-]{1,128}) > /sys/power/wake_unlock$")

        fun isValidPackage(packageName: String): Boolean = PACKAGE_REGEX.matches(packageName)

        fun isValidWakelockTag(tag: String): Boolean = TAG_REGEX.matches(tag)

        fun appopsTurnScreenOnIgnore(packageName: String): String? =
            if (isValidPackage(packageName)) {
                "cmd appops set $packageName TURN_SCREEN_ON ignore"
            } else {
                null
            }

        fun appopsTurnScreenOnAllow(packageName: String): String? =
            if (isValidPackage(packageName)) {
                "cmd appops set $packageName TURN_SCREEN_ON allow"
            } else {
                null
            }

        fun wakeUnlock(tag: String): String? =
            if (isValidWakelockTag(tag)) {
                "echo $tag > /sys/power/wake_unlock"
            } else {
                null
            }

        fun fromUserInput(input: String): RootCommandAllowlist? =
            entries.find { it.command == input }

        fun isAllowed(input: String): Boolean {
            if (entries.any { it.command == input }) return true
            if (APPOPS_IGNORE.matches(input)) return true
            if (APPOPS_ALLOW.matches(input)) return true
            if (WAKE_UNLOCK.matches(input)) return true
            return false
        }
    }
}
