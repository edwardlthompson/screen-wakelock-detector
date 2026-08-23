package com.screenwakelock.detector.updates

import android.content.Context

/** Device-local donate/update state. Do not export or peer-sync. */
interface UpdatePrefStore {
    fun lastCheckAt(): Long?
    fun lastSeenVersion(): String?
    fun dismissedVersion(): String?
    fun markChecked(now: Long, dismissedVersion: String? = null)
    fun markVersionSeen(version: String)
    fun githubChecksEnabled(): Boolean = true
    fun setGithubChecksEnabled(enabled: Boolean) {}
}

class UpdatePrefs(context: Context) : UpdatePrefStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun lastCheckAt(): Long? = prefs.getLong(KEY_LAST_CHECK, -1L).takeIf { it > 0L }

    override fun lastSeenVersion(): String? = prefs.getString(KEY_LAST_SEEN, null)

    override fun dismissedVersion(): String? = prefs.getString(KEY_DISMISSED, null)

    override fun markChecked(now: Long, dismissedVersion: String?) {
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        if (!dismissedVersion.isNullOrBlank()) {
            prefs.edit().putString(KEY_DISMISSED, dismissedVersion).apply()
        }
    }

    override fun markVersionSeen(version: String) {
        prefs.edit().putString(KEY_LAST_SEEN, version).apply()
    }

    override fun githubChecksEnabled(): Boolean = prefs.getBoolean(KEY_CHECKS, true)

    override fun setGithubChecksEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHECKS, enabled).apply()
    }

    companion object {
        private const val PREFS = "app_updates"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val KEY_LAST_SEEN = "last_seen_version"
        private const val KEY_DISMISSED = "dismissed_version"
        private const val KEY_CHECKS = "github_checks_enabled"
    }
}
