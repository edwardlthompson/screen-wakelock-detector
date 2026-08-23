package com.screenwakelock.detector.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseTest {

    @Test
    fun parseIgnoresEmptyOrMalformedPayloads() {
        assertNull(GithubRelease.parse("not-json"))
        val empty = GithubRelease.parse("""{"html_url":"https://example.com/r","assets":[]}""")
        assertEquals("https://example.com/r", empty?.htmlUrl)
        assertTrue(empty?.assets.isNullOrEmpty())
    }

    @Test
    fun parseKeepsNamedDownloadUrls() {
        val parsed = GithubRelease.parse(
            """
            {
              "html_url": "https://example.com/r",
              "tag_name": "v0.22.1",
              "assets": [
                {
                  "name": "Screen-Wakelock-Detector-1.2.19.apk",
                  "browser_download_url": "https://example.com/a.apk"
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals("https://example.com/r", parsed?.htmlUrl)
        assertEquals("Screen-Wakelock-Detector-1.2.19.apk", parsed?.assets?.first()?.name)
        assertEquals("https://example.com/a.apk", parsed?.assets?.first()?.url)
    }
}
