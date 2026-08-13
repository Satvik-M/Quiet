package com.satvikm.quiet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.FontSize
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.domain.model.LaunchableApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(viewModelScope, started, ThemeMode.SYSTEM)
    val fontFamily: StateFlow<AppFontFamily> = settingsRepository.fontFamily.stateIn(viewModelScope, started, AppFontFamily.SANS)
    val fontSize: StateFlow<FontSize> = settingsRepository.fontSize.stateIn(viewModelScope, started, FontSize.MEDIUM)
    val alignment: StateFlow<HomeAlignment> = settingsRepository.alignment.stateIn(viewModelScope, started, HomeAlignment.LEFT)
    val showScreenTime: StateFlow<Boolean> = settingsRepository.showScreenTime.stateIn(viewModelScope, started, false)

    val swipeLeftApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_LEFT)
    val swipeRightApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_RIGHT)

    private fun gestureApp(slot: GestureSlot): StateFlow<LaunchableApp?> = combine(
        appRepository.apps,
        gestureSettingsRepository.appIdFor(slot),
    ) { apps, appId -> apps.firstOrNull { it.id == appId } }.stateIn(viewModelScope, started, null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontFamily(family: AppFontFamily) {
        viewModelScope.launch { settingsRepository.setFontFamily(family) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setAlignment(alignment: HomeAlignment) {
        viewModelScope.launch { settingsRepository.setAlignment(alignment) }
    }

    fun setShowScreenTime(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowScreenTime(show) }
    }

    fun clearGestureApp(slot: GestureSlot) {
        viewModelScope.launch { gestureSettingsRepository.setAppFor(slot, null) }
    }
}
