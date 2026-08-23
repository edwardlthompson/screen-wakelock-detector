package com.screenwakelock.detector.domain.attributor

object WakelockTagDictionary {
    private val labels = mapOf(
        "com.life360.android.safetymapd" to "Life360",
        "com.huawei.health" to "Huawei Health",
        "com.huawei.health.wear" to "Huawei Health",
        "com.google.android.gms" to "Google Play services",
        "com.android.systemui" to "System UI",
    )

    fun labelFor(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return labels[packageName]
    }

    fun labelForTag(tag: String?): String? {
        val pkg = PackageFromWakelockTag.extractPackage(tag) ?: return null
        return labelFor(pkg)
    }
}
