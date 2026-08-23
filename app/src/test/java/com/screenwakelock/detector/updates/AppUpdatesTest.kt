package com.screenwakelock.detector.updates

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatesTest {

    @Test
    fun donateNudgeBlocksUpdatePrompt() = runTest {
        val prefs = MemoryUpdatePrefs().apply { lastSeen = "1.2.17" }
        val release = GithubRelease.Parsed(
            htmlUrl = ProductUpdate.RELEASES_PAGE,
            assets = listOf(
                ProductUpdate.NamedAsset(
                    "Screen-Wakelock-Detector-1.2.19.apk",
                    "https://example.com/a.apk",
                ),
            ),
        )
        val prompt = AppUpdates.decideLaunchPrompt(
            currentVersion = "1.2.18",
            prefs = prefs,
            now = ProductUpdate.MS_DAY,
            fetchLatest = { release },
        )
        assertEquals(AppUpdates.LaunchPrompt.Donate, prompt)
        assertNull(prefs.lastCheckAt())
    }

    @Test
    fun failedFetchStaysSilent() = runTest {
        val prefs = MemoryUpdatePrefs().apply { lastSeen = "1.2.18" }
        val prompt = AppUpdates.decideLaunchPrompt(
            currentVersion = "1.2.18",
            prefs = prefs,
            now = ProductUpdate.MS_DAY,
            fetchLatest = { null },
        )
        assertNull(prompt)
        assertEquals(ProductUpdate.MS_DAY, prefs.lastCheckAt())
    }

    @Test
    fun laterSilencesThatVersion() = runTest {
        val prefs = MemoryUpdatePrefs().apply {
            lastSeen = "1.2.18"
            dismissed = "1.2.19"
        }
        val release = GithubRelease.Parsed(
            htmlUrl = ProductUpdate.RELEASES_PAGE,
            assets = listOf(
                ProductUpdate.NamedAsset(
                    "Screen-Wakelock-Detector-1.2.19.apk",
                    "https://example.com/a.apk",
                ),
            ),
        )
        val prompt = AppUpdates.decideLaunchPrompt(
            currentVersion = "1.2.18",
            prefs = prefs,
            now = ProductUpdate.MS_DAY,
            fetchLatest = { release },
        )
        assertNull(prompt)
    }

    @Test
    fun firstRunRecordsVersionWithoutDonate() = runTest {
        val prefs = MemoryUpdatePrefs()
        val prompt = AppUpdates.decideLaunchPrompt(
            currentVersion = "1.2.18",
            prefs = prefs,
            now = 0L,
            fetchLatest = { null },
        )
        assertNull(prompt)
        assertEquals("1.2.18", prefs.lastSeenVersion())
        assertTrue(prompt !is AppUpdates.LaunchPrompt.Donate)
    }

    @Test
    fun githubChecksOffSkipsFetch() = runTest {
        val prefs = MemoryUpdatePrefs().apply {
            lastSeen = "1.2.18"
            checks = false
        }
        var fetched = false
        val prompt = AppUpdates.decideLaunchPrompt(
            currentVersion = "1.2.18",
            prefs = prefs,
            now = ProductUpdate.MS_DAY,
            fetchLatest = {
                fetched = true
                null
            },
        )
        assertNull(prompt)
        assertTrue(!fetched)
    }

    private class MemoryUpdatePrefs : UpdatePrefStore {
        var lastCheck: Long? = null
        var lastSeen: String? = null
        var dismissed: String? = null
        var checks: Boolean = true

        override fun lastCheckAt(): Long? = lastCheck
        override fun lastSeenVersion(): String? = lastSeen
        override fun dismissedVersion(): String? = dismissed
        override fun githubChecksEnabled(): Boolean = checks
        override fun setGithubChecksEnabled(enabled: Boolean) {
            checks = enabled
        }

        override fun markChecked(now: Long, dismissedVersion: String?) {
            lastCheck = now
            if (!dismissedVersion.isNullOrBlank()) dismissed = dismissedVersion
        }

        override fun markVersionSeen(version: String) {
            lastSeen = version
        }
    }
}
