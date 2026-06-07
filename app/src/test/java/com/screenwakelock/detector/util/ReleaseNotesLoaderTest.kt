package com.screenwakelock.detector.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesLoaderTest {

    @Test
    fun rawResourceName_usesChangelogPrefix() {
        assertEquals("changelog_1001000", ReleaseNotesLoader.rawResourceName(1001000))
    }
}
