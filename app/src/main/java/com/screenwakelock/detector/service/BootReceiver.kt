package com.screenwakelock.detector.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenwakelock.detector.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                val enabled = preferencesRepository.monitoringEnabled.first()
                if (enabled) {
                    WakeMonitorService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
