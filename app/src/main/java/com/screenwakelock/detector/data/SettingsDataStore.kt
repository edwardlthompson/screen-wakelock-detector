package com.screenwakelock.detector.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single process-wide DataStore for app settings (must not be duplicated elsewhere). */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
