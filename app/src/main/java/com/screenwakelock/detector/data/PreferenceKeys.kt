package com.screenwakelock.detector.data

import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferenceKeys {
    val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")
}
