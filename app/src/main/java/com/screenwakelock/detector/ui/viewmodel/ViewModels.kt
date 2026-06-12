package com.screenwakelock.detector.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.data.repository.WakeEventRepository
import com.screenwakelock.detector.domain.attributor.AppDisplayResolver
import com.screenwakelock.detector.domain.model.ReasonFilterGroup
import com.screenwakelock.detector.domain.model.WakeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.screenwakelock.detector.util.BackupUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wakeEventRepository: WakeEventRepository,
    private val permissionStatusRepository: PermissionStatusRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val latestWake: StateFlow<WakeEvent?> = kotlinx.coroutines.flow.combine(
        wakeEventRepository.observeAll(),
        preferencesRepository.ignoredPackages,
    ) { events, ignored ->
        events.firstOrNull { com.screenwakelock.detector.util.WakeEventFilters.isVisibleInLists(it, ignored) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ignoredPackages = preferencesRepository.ignoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val permissionHealthScore: Int = permissionStatusRepository.healthScore()

    suspend fun loadEvent(id: Long): WakeEvent? = wakeEventRepository.getById(id)

    suspend fun ignoreApp(packageName: String) =
        preferencesRepository.addIgnoredPackage(packageName)

    suspend fun unignoreApp(packageName: String) =
        preferencesRepository.removeIgnoredPackage(packageName)
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    wakeEventRepository: WakeEventRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appDisplayResolver: AppDisplayResolver,
) : ViewModel() {
    private val allEvents = wakeEventRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEventsForExport: StateFlow<List<WakeEvent>> = allEvents

    private val minWakeDurationMs = preferencesRepository.minWakeDurationMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _query = kotlinx.coroutines.flow.MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _nightOnly = kotlinx.coroutines.flow.MutableStateFlow(false)
    val nightOnly: StateFlow<Boolean> = _nightOnly

    private val _startDateMillis = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val startDateMillis: StateFlow<Long?> = _startDateMillis

    private val _endDateMillis = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val endDateMillis: StateFlow<Long?> = _endDateMillis

    private val _hourFilter = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val hourFilter: StateFlow<Int?> = _hourFilter

    private val _reasonFilterGroup = kotlinx.coroutines.flow.MutableStateFlow<ReasonFilterGroup?>(null)
    val reasonFilterGroup: StateFlow<ReasonFilterGroup?> = _reasonFilterGroup

    val ignoredPackages = preferencesRepository.ignoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val events: StateFlow<List<WakeEvent>> = kotlinx.coroutines.flow.combine(
        kotlinx.coroutines.flow.combine(
            allEvents,
            ignoredPackages,
            _query,
            _nightOnly,
        ) { events, ignored, query, nightOnly ->
            Quadruple(events, ignored, query, nightOnly)
        },
        kotlinx.coroutines.flow.combine(
            _startDateMillis,
            _endDateMillis,
            _hourFilter,
        ) { startDate, endDate, hourFilter ->
            Triple(startDate, endDate, hourFilter)
        },
        kotlinx.coroutines.flow.combine(
            _reasonFilterGroup,
            minWakeDurationMs,
        ) { reasonGroup, minDuration ->
            reasonGroup to minDuration
        },
    ) { filters, dateFilters, extraFilters ->
        val (events, ignored, query, nightOnly) = filters
        val (startDate, endDate, hourFilter) = dateFilters
        val (reasonGroup, minDuration) = extraFilters
        events.filter { event ->
            val isVisible = com.screenwakelock.detector.util.WakeEventFilters
                .isVisibleInLists(event, ignored)
            val matchesQuery = com.screenwakelock.detector.util.WakeEventFilters
                .matchesHistoryQuery(event, query, appDisplayResolver::resolveAppName)
            val hour = java.util.Calendar.getInstance().apply {
                timeInMillis = event.timestampMillis
            }.get(java.util.Calendar.HOUR_OF_DAY)
            val isNight = hour >= 23 || hour < 6
            val matchesNight = !nightOnly || isNight
            val matchesHour = hourFilter == null || hour == hourFilter
            val matchesDate = matchesDateRange(event.timestampMillis, startDate, endDate)
            val matchesReason = reasonGroup == null ||
                event.reasonCode.filterGroup() == reasonGroup
            val matchesDuration = minDuration <= 0 ||
                (event.screenOffDurationMs ?: Long.MAX_VALUE) >= minDuration
            isVisible && matchesQuery && matchesNight && matchesHour && matchesDate &&
                matchesReason && matchesDuration
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun ignoreApp(packageName: String) =
        preferencesRepository.addIgnoredPackage(packageName)

    suspend fun unignoreApp(packageName: String) =
        preferencesRepository.removeIgnoredPackage(packageName)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setNightOnly(enabled: Boolean) {
        _nightOnly.value = enabled
    }

    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        _startDateMillis.value = startMillis
        _endDateMillis.value = endMillis
    }

    fun clearDateRange() {
        _startDateMillis.value = null
        _endDateMillis.value = null
    }

    fun setHourFilter(hour: Int?) {
        _hourFilter.value = hour
    }

    fun clearHourFilter() {
        _hourFilter.value = null
    }

    fun setReasonFilterGroup(group: ReasonFilterGroup?) {
        _reasonFilterGroup.value = group
    }

    fun toggleReasonFilterGroup(group: ReasonFilterGroup) {
        _reasonFilterGroup.value = if (_reasonFilterGroup.value == group) null else group
    }

    private fun matchesDateRange(
        timestampMillis: Long,
        startMillis: Long?,
        endMillis: Long?,
    ): Boolean {
        if (startMillis == null && endMillis == null) return true
        val eventDay = dayStart(timestampMillis)
        val afterStart = startMillis == null || eventDay >= dayStart(startMillis)
        val beforeEnd = endMillis == null || eventDay <= dayStart(endMillis)
        return afterStart && beforeEnd
    }

    private fun dayStart(timestampMillis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wakeEventRepository: WakeEventRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    fun observeEvent(id: Long): StateFlow<WakeEvent?> =
        wakeEventRepository.observeById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    suspend fun loadSimilarWakes(event: WakeEvent): List<WakeEvent> {
        val pkg = event.attributedPackage ?: return emptyList()
        val since = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(7)
        return wakeEventRepository.findSimilar(
            packageName = pkg,
            channelId = event.channelId,
            sinceMillis = since,
            excludeId = event.id,
            limit = 10,
        )
    }

    suspend fun loadRootTimeline(event: WakeEvent): List<WakeEvent> {
        val pkg = event.attributedPackage ?: return emptyList()
        if (!event.rootEnhanced) return emptyList()
        val since = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(7)
        return wakeEventRepository.getRootTimelineForPackage(pkg, since)
    }

    suspend fun ignoreApp(packageName: String) =
        preferencesRepository.addIgnoredPackage(packageName)

    suspend fun unignoreApp(packageName: String) =
        preferencesRepository.removeIgnoredPackage(packageName)

    val ignoredPackages = preferencesRepository.ignoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    wakeEventRepository: WakeEventRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val events = wakeEventRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nighttimeStart = preferencesRepository.nighttimeStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 23)

    val nighttimeEnd = preferencesRepository.nighttimeEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val ignoredPackages = preferencesRepository.ignoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val nightlyBudgets = preferencesRepository.nightlyBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun setNightlyBudget(packageName: String, maxWakes: Int) =
        preferencesRepository.setNightlyBudget(packageName, maxWakes)

    suspend fun removeNightlyBudget(packageName: String) =
        preferencesRepository.removeNightlyBudget(packageName)

    suspend fun ignoreApp(packageName: String) =
        preferencesRepository.addIgnoredPackage(packageName)
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val wakeEventRepository: WakeEventRepository,
) : ViewModel() {
    val monitoringEnabled = preferencesRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val alertOnEveryWake = preferencesRepository.alertOnEveryWake
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val thresholdAlertsEnabled = preferencesRepository.thresholdAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val thresholdCount = preferencesRepository.thresholdCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val quietHoursEnabled = preferencesRepository.quietHoursEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val nighttimeStartHour = preferencesRepository.nighttimeStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 23)
    val nighttimeEndHour = preferencesRepository.nighttimeEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)
    val rootEnabled = preferencesRepository.rootEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val ignoredPackages = preferencesRepository.ignoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val retentionDays = preferencesRepository.retentionDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val minWakeDurationMs = preferencesRepository.minWakeDurationMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val monitorScheduleEnabled = preferencesRepository.monitorScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val monitorPauseStartHour = preferencesRepository.monitorPauseStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 23)
    val monitorPauseEndHour = preferencesRepository.monitorPauseEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)
    val nightlyBudgets = preferencesRepository.nightlyBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun setMonitoringEnabled(v: Boolean) = preferencesRepository.setMonitoringEnabled(v)
    suspend fun setAlertOnEveryWake(v: Boolean) = preferencesRepository.setAlertOnEveryWake(v)
    suspend fun setThresholdAlertsEnabled(v: Boolean) = preferencesRepository.setThresholdAlertsEnabled(v)
    suspend fun setThresholdCount(v: Int) = preferencesRepository.setThresholdCount(v)
    suspend fun setQuietHoursEnabled(v: Boolean) = preferencesRepository.setQuietHoursEnabled(v)
    suspend fun setNighttimeHours(start: Int, end: Int) =
        preferencesRepository.setNighttimeHours(start, end)
    suspend fun setRootEnabled(v: Boolean) = preferencesRepository.setRootEnabled(v)
    suspend fun addIgnoredPackage(pkg: String) = preferencesRepository.addIgnoredPackage(pkg)
    suspend fun removeIgnoredPackage(pkg: String) = preferencesRepository.removeIgnoredPackage(pkg)
    suspend fun setRetentionDays(days: Int) = preferencesRepository.setRetentionDays(days)
    suspend fun setMinWakeDurationMs(ms: Int) = preferencesRepository.setMinWakeDurationMs(ms)
    suspend fun setMonitorSchedule(enabled: Boolean, start: Int, end: Int) =
        preferencesRepository.setMonitorSchedule(enabled, start, end)
    suspend fun setNightlyBudget(packageName: String, maxWakes: Int) =
        preferencesRepository.setNightlyBudget(packageName, maxWakes)

    suspend fun buildBackupJson(): String {
        val settings = BackupUtils.BackupSettings(
            monitoringEnabled = preferencesRepository.monitoringEnabled.first(),
            rootEnabled = preferencesRepository.rootEnabled.first(),
            alertOnEveryWake = preferencesRepository.alertOnEveryWake.first(),
            thresholdAlertsEnabled = preferencesRepository.thresholdAlertsEnabled.first(),
            thresholdCount = preferencesRepository.thresholdCount.first(),
            nighttimeStartHour = preferencesRepository.nighttimeStartHour.first(),
            nighttimeEndHour = preferencesRepository.nighttimeEndHour.first(),
            quietHoursEnabled = preferencesRepository.quietHoursEnabled.first(),
            ignoredPackages = preferencesRepository.ignoredPackages.first(),
            retentionDays = preferencesRepository.retentionDays.first(),
            minWakeDurationMs = preferencesRepository.minWakeDurationMs.first(),
            monitorScheduleEnabled = preferencesRepository.monitorScheduleEnabled.first(),
            monitorPauseStartHour = preferencesRepository.monitorPauseStartHour.first(),
            monitorPauseEndHour = preferencesRepository.monitorPauseEndHour.first(),
            nightlyBudgets = preferencesRepository.nightlyBudgets.first(),
        )
        return BackupUtils.buildBackupJson(wakeEventRepository.getAll(), settings)
    }

    suspend fun importBackupJson(json: String) {
        val root = org.json.JSONObject(json)
        val events = BackupUtils.parseEvents(root)
        events.forEach { wakeEventRepository.insert(it) }
        val settingsObj = root.optJSONObject("settings") ?: return
        preferencesRepository.setMonitoringEnabled(settingsObj.optBoolean("monitoringEnabled", true))
        preferencesRepository.setRootEnabled(settingsObj.optBoolean("rootEnabled", false))
        preferencesRepository.setAlertOnEveryWake(settingsObj.optBoolean("alertOnEveryWake", false))
        preferencesRepository.setThresholdAlertsEnabled(
            settingsObj.optBoolean("thresholdAlertsEnabled", true),
        )
        preferencesRepository.setThresholdCount(settingsObj.optInt("thresholdCount", 3))
        preferencesRepository.setNighttimeHours(
            settingsObj.optInt("nighttimeStartHour", 23),
            settingsObj.optInt("nighttimeEndHour", 6),
        )
        preferencesRepository.setQuietHoursEnabled(settingsObj.optBoolean("quietHoursEnabled", false))
        preferencesRepository.setRetentionDays(settingsObj.optInt("retentionDays", 0))
        preferencesRepository.setMinWakeDurationMs(settingsObj.optInt("minWakeDurationMs", 0))
        preferencesRepository.setMonitorSchedule(
            settingsObj.optBoolean("monitorScheduleEnabled", false),
            settingsObj.optInt("monitorPauseStartHour", 23),
            settingsObj.optInt("monitorPauseEndHour", 7),
        )
        val ignored = settingsObj.optJSONArray("ignoredPackages")
        if (ignored != null) {
            for (i in 0 until ignored.length()) {
                preferencesRepository.addIgnoredPackage(ignored.getString(i))
            }
        }
        val budgets = settingsObj.optJSONObject("nightlyBudgets")
        if (budgets != null) {
            budgets.keys().forEach { key ->
                preferencesRepository.setNightlyBudget(key, budgets.getInt(key))
            }
        }
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val hasCompletedIntro = preferencesRepository.hasCompletedIntro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun completeIntro() = preferencesRepository.setHasCompletedIntro(true)
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val rootAvailability: com.screenwakelock.detector.root.RootAvailability,
    private val rootCommandRunner: com.screenwakelock.detector.root.RootCommandRunner,
    private val preferencesRepository: PreferencesRepository,
    private val wakeEventRepository: WakeEventRepository,
) : ViewModel() {
    val rootEnabled = preferencesRepository.rootEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun probe() = rootAvailability.probe()

    suspend fun runDiagnostics() =
        rootCommandRunner.execute(com.screenwakelock.detector.root.RootCommandAllowlist.DUMPSYS_POWER)

    suspend fun setRootEnabled(enabled: Boolean) = preferencesRepository.setRootEnabled(enabled)

    suspend fun buildDiagnosticReport(
        probeState: com.screenwakelock.detector.root.RootAvailabilityState?,
        lastDiagnostics: String?,
    ): String {
        val rootEvents = wakeEventRepository.getAll()
            .filter { it.rootEnhanced }
            .take(20)
        return com.screenwakelock.detector.root.RootDiagnosticExporter.buildReport(
            probeState,
            lastDiagnostics,
            rootEvents,
        )
    }
}
