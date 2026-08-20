package com.screenwakelock.detector.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.screenwakelock.detector.data.PreferenceKeys
import com.screenwakelock.detector.data.settingsDataStore
import com.screenwakelock.detector.domain.model.IgnorePolicy
import kotlinx.coroutines.flow.first

object WidgetPrefsReader {
    private val NIGHT_START = intPreferencesKey("nighttime_start_hour")
    private val NIGHT_END = intPreferencesKey("nighttime_end_hour")
    private val SHIELD_ENABLED = booleanPreferencesKey("shield_enabled")

    suspend fun ignorePolicy(context: Context): IgnorePolicy {
        val prefs = context.applicationContext.settingsDataStore.data.first()
        return IgnorePolicy(
            always = prefs[PreferenceKeys.IGNORED_PACKAGES] ?: emptySet(),
            nightOnly = prefs[PreferenceKeys.NIGHT_IGNORED_PACKAGES] ?: emptySet(),
            nightStartHour = prefs[NIGHT_START] ?: 23,
            nightEndHour = prefs[NIGHT_END] ?: 6,
        )
    }

    suspend fun shieldEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.settingsDataStore.data.first()
        return prefs[SHIELD_ENABLED] ?: false
    }
}
