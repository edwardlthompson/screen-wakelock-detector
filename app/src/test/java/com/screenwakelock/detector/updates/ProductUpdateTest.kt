package com.screenwakelock.detector.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUpdateTest {

    @Test
    fun dailyCheckWaitsAFullDay() {
        assertTrue(ProductUpdate.shouldCheckDaily(null, 0L))
        assertFalse(ProductUpdate.shouldCheckDaily(0L, ProductUpdate.MS_DAY - 1))
        assertTrue(ProductUpdate.shouldCheckDaily(0L, ProductUpdate.MS_DAY))
    }

    @Test
    fun apkVersionIgnoresTemplateTags() {
        assertEquals(
            "1.2.18",
            ProductUpdate.parseApkVersion("Screen-Wakelock-Detector-1.2.18.apk"),
        )
        assertEquals(
            "1.2.19",
            ProductUpdate.parseApkVersion("screen-wakelock-detector-1.2.19-foss.apk"),
        )
        assertEquals(null, ProductUpdate.parseApkVersion("v1.2.18"))
        assertEquals(null, ProductUpdate.parseApkVersion("mapping.txt"))
    }

    @Test
    fun isNewerThanCurrent() {
        assertTrue(ProductUpdate.isNewerVersion("1.2.18", "1.2.19"))
        assertFalse(ProductUpdate.isNewerVersion("1.2.18", "1.2.18"))
        assertFalse(ProductUpdate.isNewerVersion("1.2.19", "1.2.18"))
    }

    @Test
    fun donateNudgeOnlyAfterVersionChange() {
        assertFalse(ProductUpdate.shouldNudgeDonate(null, "1.2.18"))
        assertFalse(ProductUpdate.shouldNudgeDonate("1.2.18", "1.2.18"))
        assertTrue(ProductUpdate.shouldNudgeDonate("1.2.17", "1.2.18"))
    }

    @Test
    fun selectApkAssetReadsProductFilename() {
        val picked = ProductUpdate.selectApkAsset(
            listOf(
                ProductUpdate.NamedAsset("mapping.txt", "https://example.com/mapping"),
                ProductUpdate.NamedAsset(
                    "Screen-Wakelock-Detector-1.2.19.apk",
                    "https://example.com/a.apk",
                ),
            ),
        )
        assertEquals("1.2.19", picked?.version)
        assertEquals("https://example.com/a.apk", picked?.url)
    }

    @Test
    fun updatePromptSkipsDismissedVersion() {
        assertTrue(ProductUpdate.shouldPromptUpdate("1.2.18", "1.2.19", null))
        assertFalse(ProductUpdate.shouldPromptUpdate("1.2.18", "1.2.19", "1.2.19"))
        assertFalse(ProductUpdate.shouldPromptUpdate("1.2.19", "1.2.19", null))
        assertFalse(ProductUpdate.shouldPromptUpdate("1.2.18", null, null))
    }
}
