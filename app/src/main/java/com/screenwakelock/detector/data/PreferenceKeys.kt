package com.screenwakelock.detector.data

import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferenceKeys {
    val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")
    val SHIELD_ALLOWLIST_PACKAGES = stringSetPreferencesKey("shield_allowlist_packages")
    val SHIELD_DENIED_PACKAGES = stringSetPreferencesKey("shield_denied_packages")
}
