package com.screenwakelock.detector.wakeshield

/**
 * Hard-exempt packages that Wake Shield must never treat as hostile for sticky deny,
 * and should prefer to allow for lock/sleep when attributed.
 */
object ShieldExemptPackages {
    val CLOCK_ALARM = setOf(
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock",
        "net.oneplus.deskclock",
        "com.oplus.alarmclock",
        "com.android.alarmclock",
    )

    val EMERGENCY = setOf(
        "com.android.cellbroadcastreceiver",
        "com.google.android.cellbroadcastreceiver",
        "com.android.cellbroadcastreceiver.module",
        "com.oplus.cellbroadcastreceiver",
        "com.oneplus.cellbroadcastreceiver",
    )

    val SYSTEM_UI = setOf(
        "com.android.systemui",
        "com.android.keyguard",
    )

    val STATIC_HARD_EXEMPT: Set<String> = CLOCK_ALARM + EMERGENCY + SYSTEM_UI

    fun isStaticExempt(packageName: String?): Boolean =
        packageName != null && packageName in STATIC_HARD_EXEMPT
}
