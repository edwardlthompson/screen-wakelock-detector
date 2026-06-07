package com.screenwakelock.detector.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val HAS_COMPLETED_INTRO = booleanPreferencesKey("has_completed_intro")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val ROOT_ENABLED = booleanPreferencesKey("root_enabled")
        val ALERT_ON_EVERY_WAKE = booleanPreferencesKey("alert_on_every_wake")
        val THRESHOLD_ALERTS_ENABLED = booleanPreferencesKey("threshold_alerts_enabled")
        val THRESHOLD_COUNT = intPreferencesKey("threshold_count")
        val NIGHTTIME_START_HOUR = intPreferencesKey("nighttime_start_hour")
        val NIGHTTIME_END_HOUR = intPreferencesKey("nighttime_end_hour")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val MIN_WAKE_DURATION_MS = intPreferencesKey("min_wake_duration_ms")
        val MONITOR_SCHEDULE_ENABLED = booleanPreferencesKey("monitor_schedule_enabled")
        val MONITOR_PAUSE_START_HOUR = intPreferencesKey("monitor_pause_start_hour")
        val MONITOR_PAUSE_END_HOUR = intPreferencesKey("monitor_pause_end_hour")
        val NIGHTLY_BUDGETS = stringPreferencesKey("nightly_budgets")
    }

    val hasCompletedIntro: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAS_COMPLETED_INTRO] ?: false }

    val monitoringEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.MONITORING_ENABLED] ?: true }

    val rootEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ROOT_ENABLED] ?: false }

    val alertOnEveryWake: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ALERT_ON_EVERY_WAKE] ?: false }

    val thresholdAlertsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.THRESHOLD_ALERTS_ENABLED] ?: true }

    val thresholdCount: Flow<Int> =
        context.dataStore.data.map { it[Keys.THRESHOLD_COUNT] ?: 3 }

    val nighttimeStartHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.NIGHTTIME_START_HOUR] ?: 23 }

    val nighttimeEndHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.NIGHTTIME_END_HOUR] ?: 6 }

    val quietHoursEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.QUIET_HOURS_ENABLED] ?: false }

    val ignoredPackages: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.IGNORED_PACKAGES] ?: emptySet() }

    val retentionDays: Flow<Int> =
        context.dataStore.data.map { it[Keys.RETENTION_DAYS] ?: 0 }

    val minWakeDurationMs: Flow<Int> =
        context.dataStore.data.map { it[Keys.MIN_WAKE_DURATION_MS] ?: 0 }

    val monitorScheduleEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.MONITOR_SCHEDULE_ENABLED] ?: false }

    val monitorPauseStartHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.MONITOR_PAUSE_START_HOUR] ?: 23 }

    val monitorPauseEndHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.MONITOR_PAUSE_END_HOUR] ?: 7 }

    val nightlyBudgets: Flow<Map<String, Int>> =
        context.dataStore.data.map { prefs ->
            parseNightlyBudgets(prefs[Keys.NIGHTLY_BUDGETS] ?: "")
        }

    suspend fun setHasCompletedIntro(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_INTRO] = completed }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    suspend fun setRootEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ROOT_ENABLED] = enabled }
    }

    suspend fun setAlertOnEveryWake(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALERT_ON_EVERY_WAKE] = enabled }
    }

    suspend fun setThresholdAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.THRESHOLD_ALERTS_ENABLED] = enabled }
    }

    suspend fun setThresholdCount(count: Int) {
        context.dataStore.edit { it[Keys.THRESHOLD_COUNT] = count.coerceAtLeast(1) }
    }

    suspend fun setNighttimeHours(startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.NIGHTTIME_START_HOUR] = startHour.coerceIn(0, 23)
            it[Keys.NIGHTTIME_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun addIgnoredPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_PACKAGES] ?: emptySet()
            prefs[Keys.IGNORED_PACKAGES] = current + packageName
        }
    }

    suspend fun removeIgnoredPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_PACKAGES] ?: emptySet()
            prefs[Keys.IGNORED_PACKAGES] = current - packageName
        }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit {
            it[Keys.RETENTION_DAYS] = if (days in RETENTION_OPTIONS.toSet()) days else 0
        }
    }

    suspend fun setMinWakeDurationMs(ms: Int) {
        context.dataStore.edit {
            it[Keys.MIN_WAKE_DURATION_MS] = if (ms in MIN_WAKE_DURATION_OPTIONS.toSet()) ms else 0
        }
    }

    suspend fun setMonitorSchedule(enabled: Boolean, startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.MONITOR_SCHEDULE_ENABLED] = enabled
            it[Keys.MONITOR_PAUSE_START_HOUR] = startHour.coerceIn(0, 23)
            it[Keys.MONITOR_PAUSE_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setNightlyBudget(packageName: String, maxWakes: Int) {
        context.dataStore.edit { prefs ->
            val current = parseNightlyBudgets(prefs[Keys.NIGHTLY_BUDGETS] ?: "").toMutableMap()
            if (maxWakes <= 0) {
                current.remove(packageName)
            } else {
                current[packageName] = maxWakes.coerceAtLeast(1)
            }
            prefs[Keys.NIGHTLY_BUDGETS] = encodeNightlyBudgets(current)
        }
    }

    suspend fun removeNightlyBudget(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = parseNightlyBudgets(prefs[Keys.NIGHTLY_BUDGETS] ?: "").toMutableMap()
            current.remove(packageName)
            prefs[Keys.NIGHTLY_BUDGETS] = encodeNightlyBudgets(current)
        }
    }

    suspend fun nightlyBudgetFor(packageName: String): Int? {
        val budgets = nightlyBudgets.first()
        return budgets[packageName]
    }

    private fun parseNightlyBudgets(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 2) return@mapNotNull null
            val pkg = parts[0].trim()
            val count = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
            if (pkg.isEmpty()) return@mapNotNull null
            pkg to count
        }.toMap()
    }

    private fun encodeNightlyBudgets(budgets: Map<String, Int>): String =
        budgets.entries.joinToString(",") { "${it.key}:${it.value}" }

    companion object {
        val RETENTION_OPTIONS = intArrayOf(0, 30, 90, 365)
        val MIN_WAKE_DURATION_OPTIONS = intArrayOf(0, 2000, 5000)

        fun retentionLabel(days: Int): String = when (days) {
            0 -> "Off"
            30 -> "30 days"
            90 -> "90 days"
            365 -> "1 year"
            else -> "$days days"
        }

        fun minWakeDurationLabel(ms: Int): String = when (ms) {
            0 -> "Show all wakes"
            2000 -> "Hide wakes under 2s"
            5000 -> "Hide wakes under 5s"
            else -> "${ms}ms"
        }
    }
}
