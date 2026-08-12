package com.satvikm.quiet.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
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
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val apps: StateFlow<List<LaunchableApp>> = appRepository.apps.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = emptyList(),
    )

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favorites
        .combine(apps) { favorites, _ -> favorites.map { it.appId }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    /** Favorites in the user's chosen order, cross-referenced against live app data. */
    val favorites: StateFlow<List<LaunchableApp>> = combine(apps, favoritesRepository.favorites) { allApps, favoriteEntities ->
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

    fun launch(app: LaunchableApp) {
        appRepository.launch(app)
    }

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(app) }
    }
}
