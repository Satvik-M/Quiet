package com.satvikm.quiet.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.domain.model.LaunchableApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favorites
        .combine(appRepository.apps) { favorites, _ -> favorites.map { it.appId }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    /** Ranked so prefix matches ("cal" -> Calculator, Calendar) beat substring matches. */
    val filteredApps: StateFlow<List<LaunchableApp>> = appRepository.apps
        .combine(query) { apps, text -> rankByQuery(apps, text) }
        .stateIn(viewModelScope, started, emptyList())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun launch(app: LaunchableApp) {
        appRepository.launch(app)
    }

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(app) }
    }

    private fun rankByQuery(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return apps.sortedBy { it.label.lowercase() }
        }
        val q = trimmed.lowercase()
        return apps
            .mapNotNull { app ->
                val label = app.label.lowercase()
                val rank = when {
                    label == q -> 0
                    label.startsWith(q) -> 1
                    label.split(' ').any { it.startsWith(q) } -> 2
                    label.contains(q) -> 3
                    else -> null
                }
                rank?.let { Triple(app, it, label) }
            }
            .sortedWith(compareBy({ it.second }, { it.third }))
            .map { it.first }
    }
}
