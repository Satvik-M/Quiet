package com.satvikm.quiet.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.batteryLevelFlow
import com.satvikm.quiet.util.currentTimeFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}
