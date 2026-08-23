package com.screenwakelock.detector.data

import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferenceKeys {
    val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")
    val NIGHT_IGNORED_PACKAGES = stringSetPreferencesKey("night_ignored_packages")
    val SHIELD_ALLOWLIST_PACKAGES = stringSetPreferencesKey("shield_allowlist_packages")
    val SHIELD_DENIED_PACKAGES = stringSetPreferencesKey("shield_denied_packages")
    val SHIELD_NIGHT_ONLY_PACKAGES = stringSetPreferencesKey("shield_night_only_packages")
    val SHIELD_NEVER_TONIGHT_PACKAGES = stringSetPreferencesKey("shield_never_tonight_packages")
}
