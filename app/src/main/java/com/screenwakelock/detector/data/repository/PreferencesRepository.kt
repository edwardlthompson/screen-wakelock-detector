package com.screenwakelock.detector.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
}
