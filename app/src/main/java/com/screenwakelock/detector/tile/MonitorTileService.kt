package com.screenwakelock.detector.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.screenwakelock.detector.R
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.service.WakeMonitorService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitorTileService : TileService() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val enabled = preferencesRepository.monitoringEnabled.first()
            val newState = !enabled
            preferencesRepository.setMonitoringEnabled(newState)
            if (newState) {
                WakeMonitorService.start(applicationContext)
            } else {
                WakeMonitorService.stop(applicationContext)
            }
            updateTileState(newState)
        }
    }

    private fun updateTile() {
        scope.launch {
            val enabled = preferencesRepository.monitoringEnabled.first()
            updateTileState(enabled)
        }
    }

    private fun updateTileState(enabled: Boolean) {
        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (enabled) {
                    getString(R.string.monitoring_notification_title)
                } else {
                    getString(R.string.monitoring_paused)
                }
            }
            updateTile()
        }
    }
}
