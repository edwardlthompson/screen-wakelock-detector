package com.screenwakelock.detector.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.data.repository.WakeEventRepository
import com.screenwakelock.detector.domain.model.WakeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wakeEventRepository: WakeEventRepository,
) : ViewModel() {
    val latestWake: StateFlow<WakeEvent?> = wakeEventRepository.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun loadEvent(id: Long): WakeEvent? = wakeEventRepository.getById(id)
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    wakeEventRepository: WakeEventRepository,
) : ViewModel() {
    private val allEvents = wakeEventRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val events: StateFlow<List<WakeEvent>> = kotlinx.coroutines.flow.combine(
        kotlinx.coroutines.flow.combine(
            allEvents,
            _query,
            _nightOnly,
        ) { events, query, nightOnly ->
            Triple(events, query, nightOnly)
        },
        kotlinx.coroutines.flow.combine(
            _startDateMillis,
            _endDateMillis,
            _hourFilter,
        ) { startDate, endDate, hourFilter ->
            Triple(startDate, endDate, hourFilter)
        },
    ) { filters, dateFilters ->
        val (events, query, nightOnly) = filters
        val (startDate, endDate, hourFilter) = dateFilters
        events.filter { event ->
            val matchesQuery = query.isBlank() ||
                event.displayAppName.contains(query, ignoreCase = true) ||
                event.attributedPackage?.contains(query, ignoreCase = true) == true ||
                event.displayChannel?.contains(query, ignoreCase = true) == true
            val hour = java.util.Calendar.getInstance().apply {
                timeInMillis = event.timestampMillis
            }.get(java.util.Calendar.HOUR_OF_DAY)
            val isNight = hour >= 23 || hour < 6
            val matchesNight = !nightOnly || isNight
            val matchesHour = hourFilter == null || hour == hourFilter
            val matchesDate = matchesDateRange(event.timestampMillis, startDate, endDate)
            matchesQuery && matchesNight && matchesHour && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
) : ViewModel() {
    fun observeEvent(id: Long): StateFlow<WakeEvent?> =
        wakeEventRepository.observeById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    wakeEventRepository: WakeEventRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val events = wakeEventRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nighttimeStart = preferencesRepository.nighttimeStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 23)

    val nighttimeEnd = preferencesRepository.nighttimeEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
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

    suspend fun setMonitoringEnabled(v: Boolean) = preferencesRepository.setMonitoringEnabled(v)
    suspend fun setAlertOnEveryWake(v: Boolean) = preferencesRepository.setAlertOnEveryWake(v)
    suspend fun setThresholdAlertsEnabled(v: Boolean) = preferencesRepository.setThresholdAlertsEnabled(v)
    suspend fun setThresholdCount(v: Int) = preferencesRepository.setThresholdCount(v)
    suspend fun setQuietHoursEnabled(v: Boolean) = preferencesRepository.setQuietHoursEnabled(v)
    suspend fun setNighttimeHours(start: Int, end: Int) =
        preferencesRepository.setNighttimeHours(start, end)
    suspend fun setRootEnabled(v: Boolean) = preferencesRepository.setRootEnabled(v)
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
) : ViewModel() {
    val rootEnabled = preferencesRepository.rootEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun probe() = rootAvailability.probe()

    suspend fun runDiagnostics() =
        rootCommandRunner.execute(com.screenwakelock.detector.root.RootCommandAllowlist.DUMPSYS_POWER)

    suspend fun setRootEnabled(enabled: Boolean) = preferencesRepository.setRootEnabled(enabled)
}
