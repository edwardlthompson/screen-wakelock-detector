package com.screenwakelock.detector.util

import android.content.Context

object ReleaseNotesLoader {
    fun rawResourceName(versionCode: Int): String = "changelog_$versionCode"

    fun load(context: Context, versionCode: Int): String? {
        val resId = context.resources.getIdentifier(
            rawResourceName(versionCode),
            "raw",
            context.packageName,
        )
        if (resId == 0) return null
        return context.resources.openRawResource(resId).bufferedReader().use { it.readText().trim() }
            .takeIf { it.isNotEmpty() }
    }
}
