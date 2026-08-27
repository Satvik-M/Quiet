package com.satvikm.quiet.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.focus.FocusScheduleRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.usage.UsageRepository
import com.satvikm.quiet.data.workprofile.WorkProfileMode
import com.satvikm.quiet.data.workprofile.WorkProfileRepository
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.batteryLevelFlow
import com.satvikm.quiet.util.currentTimeFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val workProfileRepository: WorkProfileRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val activeProfile: StateFlow<WorkProfileMode> =
        workProfileRepository.activeProfile.stateIn(viewModelScope, started, WorkProfileMode.NORMAL)
    val paused: StateFlow<Boolean> = workProfileRepository.paused.stateIn(viewModelScope, started, false)

    /** Favorites in the user's chosen order, cross-referenced against live app data. Draws from Work Mode's own favorites while Work Mode is active, else the Normal-mode favorites. */
    val favorites: StateFlow<List<LaunchableApp>> = activeProfile.flatMapLatest { profile ->
        when (profile) {
            WorkProfileMode.NORMAL -> combine(
                appRepository.apps,
                favoritesRepository.favorites,
            ) { allApps, favoriteEntities ->
                val byId = allApps.associateBy { it.id }
                favoriteEntities.mapNotNull { byId[it.appId] }
            }
            WorkProfileMode.WORK -> combine(
                appRepository.apps,
                workProfileRepository.favorites,
            ) { allApps, favoriteEntities ->
                val byId = allApps.associateBy { it.id }
                favoriteEntities.mapNotNull { byId[it.appId] }
            }
        }
    }.stateIn(viewModelScope, started, emptyList())

    val currentTime: StateFlow<ZonedDateTime> = currentTimeFlow(context).stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = ZonedDateTime.now(),
    )

    val batteryPercent: StateFlow<Int> = batteryLevelFlow(context).stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = 0,
    )

    val manualFocusActive: StateFlow<Boolean> = settingsRepository.manualFocusActive.stateIn(viewModelScope, started, false)
    val manualFocusEndsAtMillis: StateFlow<Long?> = settingsRepository.manualFocusEndsAtMillis.stateIn(viewModelScope, started, null)
    val manualFocusLocked: StateFlow<Boolean> = settingsRepository.manualFocusLocked.stateIn(viewModelScope, started, false)
    val focusActive: StateFlow<Boolean> = combine(
        focusScheduleRepository.schedules,
        currentTime,
        manualFocusActive,
    ) { schedules, now, manual ->
        manual || FocusScheduleRepository.isActiveAt(schedules, now)
    }.stateIn(viewModelScope, started, false)
    val showFocusStatus: StateFlow<Boolean> = settingsRepository.showFocusStatus.stateIn(viewModelScope, started, false)

    val swipeLeftApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_LEFT)
    val swipeRightApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_RIGHT)

    val alignment: StateFlow<HomeAlignment> = settingsRepository.alignment.stateIn(viewModelScope, started, HomeAlignment.LEFT)
    val showScreenTime: StateFlow<Boolean> = settingsRepository.showScreenTime.stateIn(viewModelScope, started, false)
    val showMutedCount: StateFlow<Boolean> = settingsRepository.showMutedCount.stateIn(viewModelScope, started, false)

    private val _screenTimeMillis = MutableStateFlow<Long?>(null)
    val screenTimeMillis: StateFlow<Long?> = _screenTimeMillis.asStateFlow()

    private val _mutedCountToday = MutableStateFlow(0)
    val mutedCountToday: StateFlow<Int> = _mutedCountToday.asStateFlow()

    init {
        refreshScreenTime()
        refreshMutedCount()
    }

    private fun gestureApp(slot: GestureSlot): StateFlow<LaunchableApp?> = combine(
        appRepository.apps,
        gestureSettingsRepository.appIdFor(slot),
    ) { apps, appId -> apps.firstOrNull { it.id == appId } }.stateIn(viewModelScope, started, null)

    fun launch(app: LaunchableApp) {
        appRepository.launch(app)
    }

    fun reorderFavorites(appIdsInOrder: List<String>) {
        viewModelScope.launch {
            if (activeProfile.value == WorkProfileMode.WORK) {
                workProfileRepository.reorderFavorites(appIdsInOrder)
            } else {
                favoritesRepository.reorder(appIdsInOrder)
            }
        }
    }

    /** Toggles between Normal and Work Mode. Instant — no dialog, no timer, no lock. */
    fun switchProfile() {
        viewModelScope.launch {
            val next = if (activeProfile.value == WorkProfileMode.WORK) WorkProfileMode.NORMAL else WorkProfileMode.WORK
            workProfileRepository.switchTo(next)
        }
    }

    /** Only meaningful while Work Mode is active; ignored otherwise so Normal mode can never end up "paused". */
    fun togglePause() {
        if (activeProfile.value != WorkProfileMode.WORK) return
        viewModelScope.launch { workProfileRepository.setPaused(!paused.value) }
    }

    fun refreshScreenTime() {
        if (!usageRepository.isUsageAccessGranted()) {
            _screenTimeMillis.value = null
            return
        }
        viewModelScope.launch { _screenTimeMillis.value = usageRepository.today().totalMillis }
    }

    fun refreshMutedCount() {
        viewModelScope.launch { _mutedCountToday.value = notificationMuteRepository.mutedCountToday() }
    }

    fun startFocus(durationMinutes: Int?, locked: Boolean) {
        viewModelScope.launch { settingsRepository.startManualFocus(durationMinutes, locked) }
    }

    /** Attempts to end the current manual focus session; [onResult] reports whether it actually ended (false if it's locked and still running), so the caller can explain why nothing happened. */
    fun endFocus(onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(settingsRepository.endManualFocusIfAllowed()) }
    }
}
