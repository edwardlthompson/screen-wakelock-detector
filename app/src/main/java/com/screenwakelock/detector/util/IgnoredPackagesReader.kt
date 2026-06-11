package com.screenwakelock.detector.util

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.screenwakelock.detector.data.settingsDataStore
import kotlinx.coroutines.flow.first

object IgnoredPackagesReader {
    private val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")

    suspend fun read(context: Context): Set<String> {
        val prefs = context.applicationContext.settingsDataStore.data.first()
        return prefs[IGNORED_PACKAGES] ?: emptySet()
    }
}
