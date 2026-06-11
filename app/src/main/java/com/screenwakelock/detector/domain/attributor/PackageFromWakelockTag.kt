package com.screenwakelock.detector.domain.attributor

object PackageFromWakelockTag {
    private val PACKAGE_PATTERN =
        Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$""")

    /**
     * Returns the package prefix from tags like `com.example.app:notification`.
     * Does not require the package to be installed.
     */
    fun extractPackage(tag: String?): String? {
        if (tag.isNullOrBlank()) return null
        val prefix = tag.substringBefore(':').trim()
        if (prefix.isEmpty() || !PACKAGE_PATTERN.matches(prefix)) return null
        return prefix
    }
}
