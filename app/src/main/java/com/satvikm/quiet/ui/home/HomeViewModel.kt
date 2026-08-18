package com.satvikm.quiet.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.usage.UsageRepository
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.batteryLevelFlow
import com.satvikm.quiet.util.currentTimeFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    /** Favorites in the user's chosen order, cross-referenced against live app data. */
    val favorites: StateFlow<List<LaunchableApp>> = combine(
        appRepository.apps,
        favoritesRepository.favorites,
    ) { allApps, favoriteEntities ->
        val byId = allApps.associateBy { it.id }
        favoriteEntities.mapNotNull { byId[it.appId] }
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
        viewModelScope.launch { favoritesRepository.reorder(appIdsInOrder) }
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
}
