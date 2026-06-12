package com.screenwakelock.detector.util

import android.content.Context
import com.screenwakelock.detector.data.PreferenceKeys
import com.screenwakelock.detector.data.settingsDataStore
import kotlinx.coroutines.flow.first

object IgnoredPackagesReader {
    suspend fun read(context: Context): Set<String> {
        val prefs = context.applicationContext.settingsDataStore.data.first()
        return prefs[PreferenceKeys.IGNORED_PACKAGES] ?: emptySet()
    }
}
