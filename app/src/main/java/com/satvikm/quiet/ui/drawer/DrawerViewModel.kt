package com.satvikm.quiet.ui.drawer

import android.content.ActivityNotFoundException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppOverridesRepository
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.workprofile.WorkProfileManager
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

/** Which side of a real OS work-profile split the drawer is currently showing. */
enum class DrawerProfile { PERSONAL, WORK }

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val overridesRepository: AppOverridesRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val blocklistRepository: BlocklistRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
    private val workProfileManager: WorkProfileManager,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    private val profile = MutableStateFlow(DrawerProfile.PERSONAL)
    val selectedProfile: StateFlow<DrawerProfile> = profile.asStateFlow()

    /**
     * Whether a work profile exists at all. Computed once — provisioning a work profile isn't
     * something that happens while this drawer is open, so this doesn't need to be reactive to
     * a live broadcast.
     */
    val hasWorkProfile: StateFlow<Boolean> =
        MutableStateFlow(workProfileManager.hasWorkProfile()).asStateFlow()

    /**
     * Reflects live quiet-mode state for the work profile. Unlike [hasWorkProfile], this *can*
     * change while the drawer is open (e.g. the user paused/resumed it from this same screen, or
     * from system Settings), so it's refreshed explicitly via [refreshWorkQuietMode] — called on
     * `ON_RESUME` from the screen (see DrawerScreen) and right after a pause/resume attempt here
     * — rather than wired to a live broadcast receiver, which isn't practical to verify without
     * a physical device/CI harness.
     */
    private val _workQuietModeEnabled = MutableStateFlow(false)
    val workQuietModeEnabled: StateFlow<Boolean> = _workQuietModeEnabled.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favorites
        .combine(appRepository.apps) { favorites, _ -> favorites.map { it.appId }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    val blockedPackageNames: StateFlow<Set<String>> = blocklistRepository.blockedApps
        .map { blocked -> blocked.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    val mutedPackageNames: StateFlow<Set<String>> = notificationMuteRepository.mutedApps
        .map { muted -> muted.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, started, emptySet())

    init {
        refreshWorkQuietMode()
    }

    /**
     * Ranked so prefix matches ("cal" -> Calculator, Calendar) beat substring matches. Restricted
     * to whichever OS user profile ([DrawerProfile.PERSONAL] or [DrawerProfile.WORK]) is
     * currently selected — a straight profile-membership filter (every app under that
     * [android.os.UserHandle], no curation), not any kind of allowlist.
     */
    val filteredApps: StateFlow<List<LaunchableApp>> = combine(
        appRepository.apps,
        query,
        profile,
    ) { apps, text, selectedProfile ->
        val handle = when (selectedProfile) {
            DrawerProfile.PERSONAL -> workProfileManager.personalUserHandle()
            DrawerProfile.WORK -> workProfileManager.workUserHandle()
        }
        val inProfile = if (handle == null) emptyList() else apps.filter { it.userHandle == handle }
        filterAndRank(inProfile, text)
    }.stateIn(viewModelScope, started, emptyList())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun selectProfile(newProfile: DrawerProfile) {
        profile.value = newProfile
    }

    fun refreshWorkQuietMode() {
        val handle = workProfileManager.workUserHandle() ?: return
        _workQuietModeEnabled.value = workProfileManager.isQuietModeEnabled(handle)
    }

    /** [onResult] reports whether the OS actually completed the action, so the caller can explain when it didn't (see [WorkProfileManager.resumeWorkApps]'s known API 28-30 limitation). */
    fun togglePauseWorkApps(onResult: (Boolean) -> Unit) {
        val handle = workProfileManager.workUserHandle() ?: return
        viewModelScope.launch {
            val succeeded = if (_workQuietModeEnabled.value) {
                workProfileManager.resumeWorkApps(handle)
            } else {
                workProfileManager.pauseWorkApps(handle)
            }
            refreshWorkQuietMode()
            onResult(succeeded)
        }
    }

    /**
     * [onFailure] is invoked if launching threw — e.g. tapping a Work-tab app while the work
     * profile is frozen (paused). [AppRepository.launch] itself doesn't catch this (it's a
     * thin wrapper over [android.content.pm.LauncherApps.startMainActivity]), so this is the
     * first point in the call chain able to safely surface it instead of crashing.
     */
    fun launch(app: LaunchableApp, onFailure: () -> Unit = {}) {
        try {
            appRepository.launch(app)
        } catch (e: SecurityException) {
            onFailure()
        } catch (e: ActivityNotFoundException) {
            onFailure()
        }
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

    /** Turning friction on is immediate; turning it off starts a cooldown (see [BlocklistRepository.requestRemoval]) rather than removing it on the spot. */
    fun setBlocked(app: LaunchableApp, blocked: Boolean) {
        viewModelScope.launch {
            if (blocked) blocklistRepository.setBlocked(app.packageName) else blocklistRepository.requestRemoval(app.packageName)
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
