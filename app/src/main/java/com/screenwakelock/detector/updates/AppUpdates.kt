package com.screenwakelock.detector.updates

object AppUpdates {
    sealed class LaunchPrompt {
        data object Donate : LaunchPrompt()
        data class Update(val version: String, val url: String) : LaunchPrompt()
    }

    suspend fun decideLaunchPrompt(
        currentVersion: String,
        prefs: UpdatePrefStore,
        now: Long,
        fetchLatest: suspend () -> GithubRelease.Parsed?,
        forceCheck: Boolean = false,
    ): LaunchPrompt? {
        if (ProductUpdate.shouldNudgeDonate(prefs.lastSeenVersion(), currentVersion)) {
            return LaunchPrompt.Donate
        }
        prefs.markVersionSeen(currentVersion)
        if (!prefs.githubChecksEnabled()) {
            return null
        }
        if (!forceCheck && !ProductUpdate.shouldCheckDaily(prefs.lastCheckAt(), now)) {
            return null
        }
        val release = try {
            fetchLatest()
        } catch (_: Exception) {
            null
        }
        prefs.markChecked(now)
        val asset = release?.let { ProductUpdate.selectApkAsset(it.assets) }
        val latest = asset?.version
        if (
            ProductUpdate.shouldPromptUpdate(currentVersion, latest, prefs.dismissedVersion()) &&
            latest != null
        ) {
            val url = asset.url.ifBlank { null }
                ?: release?.htmlUrl?.ifBlank { null }
                ?: ProductUpdate.RELEASES_PAGE
            return LaunchPrompt.Update(latest, url)
        }
        return null
    }
}
