package com.screenwakelock.detector.domain.insights

object ShieldDigestCopy {
    fun body(counts: ShieldWeekCounts): String =
        "${counts.shielded} shielded · ${counts.allowed} allowed this week"
}
