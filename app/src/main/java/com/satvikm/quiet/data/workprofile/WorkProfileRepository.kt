package com.satvikm.quiet.data.workprofile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.satvikm.quiet.domain.model.LaunchableApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workProfileSettingsDataStore by preferencesDataStore(name = "work_profile_settings")

/** Which curated set the drawer/home currently draw from. Named "WorkProfile" internally to match this feature's Room tables and DataStore file, but every user-facing string calls this "Work Mode"/"Normal Mode" — never "Work Profile", to avoid clashing with Android's real OS-level Work Profile concept and this app's separate "Focus Mode" feature. */
enum class WorkProfileMode { NORMAL, WORK }

/**
 * Owns Work Mode's dedicated app set: a curated (allowlist-only) drawer, its own home-screen
 * favorites, and the instant active/paused state — all deliberately separate from Focus Mode
 * (which is purely behavioral, see [com.satvikm.quiet.data.focus.FocusModeOrchestrator]) and
 * from the shared `app_settings` DataStore's manual-focus commitment-lock state.
 *
 * Unlike Focus Mode, switching profiles and pausing are instant: no dialog, no timer, no lock.
 */
@Singleton
class WorkProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteDao: WorkProfileFavoriteDao,
    private val allowedAppDao: WorkProfileAllowedAppDao,
) {
    private object Keys {
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val PAUSED = booleanPreferencesKey("paused")
    }

    val activeProfile: Flow<WorkProfileMode> = context.workProfileSettingsDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE]?.let { runCatching { WorkProfileMode.valueOf(it) }.getOrNull() } ?: WorkProfileMode.NORMAL
    }

    /** Only meaningful while [activeProfile] is [WorkProfileMode.WORK] — see class doc. Pausing lifts the drawer allowlist only; home favorites are unaffected. */
    val paused: Flow<Boolean> = context.workProfileSettingsDataStore.data.map { it[Keys.PAUSED] ?: false }

    /**
     * Switches the active profile immediately. Always resets [paused] to false: switching to
     * NORMAL clears it for next time, and switching to WORK means Work Mode always starts
     * unpaused, per the acceptance criteria for this feature.
     */
    suspend fun switchTo(profile: WorkProfileMode) {
        context.workProfileSettingsDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE] = profile.name
            prefs[Keys.PAUSED] = false
        }
    }

    suspend fun setPaused(paused: Boolean) {
        context.workProfileSettingsDataStore.edit { it[Keys.PAUSED] = paused }
    }

    /** Ordered by the user's chosen position, not alphabetically. */
    val favorites: Flow<List<WorkProfileFavoriteEntity>> = favoriteDao.observeAll()

    suspend fun toggleFavorite(app: LaunchableApp) {
        if (favoriteDao.isFavorite(app.id)) {
            favoriteDao.delete(app.id)
        } else if (favoriteDao.count() < MAX_FAVORITES) {
            favoriteDao.insert(WorkProfileFavoriteEntity(appId = app.id, position = favoriteDao.count()))
        }
    }

    suspend fun reorderFavorites(appIdsInOrder: List<String>) {
        favoriteDao.reorder(appIdsInOrder)
    }

    /** Replaces every Work Mode favorite with [entities] — used by backup restore. */
    suspend fun replaceAllFavorites(entities: List<WorkProfileFavoriteEntity>) {
        favoriteDao.deleteAll()
        entities.forEach { favoriteDao.insert(it) }
    }

    val allowedApps: Flow<List<WorkProfileAllowedAppEntity>> = allowedAppDao.observeAll()
    val allowedAppIds: Flow<Set<String>> = allowedApps.map { list -> list.map { it.appId }.toSet() }

    suspend fun setAllowed(app: LaunchableApp, allowed: Boolean) {
        if (allowed) allowedAppDao.insert(WorkProfileAllowedAppEntity(appId = app.id)) else allowedAppDao.delete(app.id)
    }

    /** Replaces every Work Mode allowlist entry with [entities] — used by backup restore. */
    suspend fun replaceAllAllowedApps(entities: List<WorkProfileAllowedAppEntity>) {
        allowedAppDao.deleteAll()
        entities.forEach { allowedAppDao.insert(it) }
    }

    companion object {
        /** Mirrors [com.satvikm.quiet.data.favorites.FavoritesRepository.MAX_FAVORITES]. */
        const val MAX_FAVORITES = 8
    }
}
