package com.screenwakelock.detector.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.screenwakelock.detector.R
import com.screenwakelock.detector.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShieldTileService : TileService() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render(preferencesRepository.shieldEnabled.first()) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val next = !preferencesRepository.shieldEnabled.first()
            preferencesRepository.setShieldEnabled(next)
            render(next)
        }
    }

    private fun render(armed: Boolean) {
        qsTile?.apply {
            state = if (armed) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_shield_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (armed) "Armed" else "Off"
            }
            updateTile()
        }
    }
}
