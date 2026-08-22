package com.satvikm.quiet.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.satvikm.quiet.R
import com.satvikm.quiet.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Toggles the same ad-hoc "Focus now" override as the home screen — see [HomeViewModel][com.satvikm.quiet.ui.home.HomeViewModel.startFocus]. A locked session started from the home screen can't be ended from here either. */
@AndroidEntryPoint
class FocusTileService : TileService() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private var listeningScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        listeningScope = scope
        settingsRepository.manualFocusActive
            .onEach { active -> updateTile(active) }
            .launchIn(scope)
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        listeningScope?.launch {
            val current = settingsRepository.manualFocusActive.first()
            if (current) {
                settingsRepository.endManualFocusIfAllowed()
            } else {
                settingsRepository.startManualFocus(durationMinutes = null, locked = false)
            }
        }
    }

    private fun updateTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.quick_settings_focus_tile_label)
            updateTile()
        }
    }
}
