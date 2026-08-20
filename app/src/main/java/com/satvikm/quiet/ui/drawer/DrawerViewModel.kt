package com.satvikm.quiet.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppOverridesRepository
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.domain.model.LaunchableApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val overridesRepository: AppOverridesRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val blocklistRepository: BlocklistRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favorites
        .combine(appRepository.apps) { favorites, _ -> favorites.map { it.appId }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    val blockedPackageNames: StateFlow<Set<String>> = blocklistRepository.blockedApps
        .map { blocked -> blocked.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    val mutedPackageNames: StateFlow<Set<String>> = notificationMuteRepository.mutedApps
        .map { muted -> muted.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    /** Ranked so prefix matches ("cal" -> Calculator, Calendar) beat substring matches. */
    val filteredApps: StateFlow<List<LaunchableApp>> = appRepository.apps
        .combine(query) { apps, text -> filterAndRank(apps, text) }
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

    fun setHidden(app: LaunchableApp, hidden: Boolean) {
        viewModelScope.launch { overridesRepository.setHidden(app, hidden) }
    }

    fun rename(app: LaunchableApp, newLabel: String?) {
        viewModelScope.launch { overridesRepository.setCustomLabel(app, newLabel) }
    }

    fun setGestureApp(slot: GestureSlot, app: LaunchableApp) {
        viewModelScope.launch { gestureSettingsRepository.setAppFor(slot, app.id) }
    }

    fun setBlocked(app: LaunchableApp, blocked: Boolean) {
        viewModelScope.launch {
            if (blocked) blocklistRepository.setBlocked(app.packageName) else blocklistRepository.unblock(app.packageName)
        }
    }

    fun setMuted(app: LaunchableApp, muted: Boolean) {
        viewModelScope.launch {
            if (muted) notificationMuteRepository.mute(app.packageName) else notificationMuteRepository.unmute(app.packageName)
        }
    }

    private fun filterAndRank(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
        val trimmed = query.trim()

        val visible = apps.filterNot { it.isHidden }
        if (trimmed.isEmpty()) {
            return visible.sortedBy { it.displayLabel.lowercase() }
        }
        val q = trimmed.lowercase()
        return visible
            .mapNotNull { app ->
                val label = app.displayLabel.lowercase()
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
