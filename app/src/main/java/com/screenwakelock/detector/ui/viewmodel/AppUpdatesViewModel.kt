package com.screenwakelock.detector.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenwakelock.detector.BuildConfig
import com.screenwakelock.detector.updates.AppUpdates
import com.screenwakelock.detector.updates.GithubRelease
import com.screenwakelock.detector.updates.UpdatePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUpdatesViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    private val prefs = UpdatePrefs(context)
    private val _prompt = MutableStateFlow<AppUpdates.LaunchPrompt?>(null)
    val prompt: StateFlow<AppUpdates.LaunchPrompt?> = _prompt.asStateFlow()

    @Volatile
    private var started = false

    fun onLaunch() {
        if (started) return
        started = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = AppUpdates.decideLaunchPrompt(
                currentVersion = BuildConfig.VERSION_NAME,
                prefs = prefs,
                now = System.currentTimeMillis(),
                fetchLatest = { GithubRelease.fetchLatest(BuildConfig.VERSION_NAME) },
            )
            _prompt.value = result
        }
    }

    fun checkNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = AppUpdates.decideLaunchPrompt(
                currentVersion = BuildConfig.VERSION_NAME,
                prefs = prefs,
                now = System.currentTimeMillis(),
                fetchLatest = { GithubRelease.fetchLatest(BuildConfig.VERSION_NAME) },
                forceCheck = true,
            )
            _prompt.value = result
        }
    }

    fun onDonateFinished() {
        prefs.markVersionSeen(BuildConfig.VERSION_NAME)
        _prompt.value = null
    }

    fun onUpdateFinished(version: String) {
        prefs.markChecked(System.currentTimeMillis(), version)
        _prompt.value = null
    }
}
