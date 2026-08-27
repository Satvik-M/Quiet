package com.satvikm.quiet.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.satvikm.quiet.data.apps.AppDatabase
import com.satvikm.quiet.data.apps.AppOverrideEntity
import com.satvikm.quiet.data.apps.AppOverridesRepository
import com.satvikm.quiet.data.block.BlockedAppEntity
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.favorites.FavoriteEntity
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.focus.FocusScheduleEntity
import com.satvikm.quiet.data.focus.FocusScheduleRepository
import com.satvikm.quiet.data.notifications.MutedAppEntity
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.FontSize
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.data.workprofile.WorkProfileAllowedAppEntity
import com.satvikm.quiet.data.workprofile.WorkProfileFavoriteEntity
import com.satvikm.quiet.data.workprofile.WorkProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up and restores everything the user configured: DataStore preferences plus every
 * Room table that represents a deliberate choice (friction list, muted apps, focus schedules,
 * favorites, hidden/renamed apps, Work Mode's favorites and allowlist). Deliberately excludes
 * derived/transient state — the muted-notification digest log, app-open/grace history, the
 * cached app list itself (which rebuilds from the system on its own), and — same policy as
 * `manualFocusActive`/`manualFocusEndsAtMillis` — Work Mode's currently-active-profile and
 * paused flags, which are instant runtime state, not a deliberate long-term choice.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val blocklistRepository: BlocklistRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appOverridesRepository: AppOverridesRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
    private val workProfileRepository: WorkProfileRepository,
) {
    suspend fun exportTo(uri: Uri) {
        val bytes = buildBackupJson().toString(2).toByteArray()
        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Could not open $uri for writing")
        stream.use { it.write(bytes) }
    }

    suspend fun importFrom(uri: Uri) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not open $uri for reading")
        val text = stream.use { it.readBytes().decodeToString() }
        applyBackupJson(JSONObject(text))
    }

    private suspend fun buildBackupJson(): JSONObject {
        val settings = JSONObject()
            .put("themeMode", settingsRepository.themeMode.first().name)
            .put("fontFamily", settingsRepository.fontFamily.first().name)
            .put("fontSize", settingsRepository.fontSize.first().name)
            .put("alignment", settingsRepository.alignment.first().name)
            .put("showScreenTime", settingsRepository.showScreenTime.first())
            .put("showMutedCount", settingsRepository.showMutedCount.first())
            .put("grayscaleEnabled", settingsRepository.grayscaleEnabled.first())
            .put("focusAutomationEnabled", settingsRepository.focusAutomationEnabled.first())
            .put("showFocusStatus", settingsRepository.showFocusStatus.first())
            .put("notificationDigestEnabled", settingsRepository.notificationDigestEnabled.first())
            .put("dailyGoalMinutes", settingsRepository.dailyGoalMinutes.first() ?: JSONObject.NULL)

        val favorites = JSONArray()
        favoritesRepository.favorites.first().forEach { fav ->
            favorites.put(JSONObject().put("appId", fav.appId).put("position", fav.position))
        }

        val hiddenApps = JSONArray()
        appOverridesRepository.overrides.first().forEach { override ->
            hiddenApps.put(
                JSONObject()
                    .put("appId", override.appId)
                    .put("customLabel", override.customLabel ?: JSONObject.NULL)
                    .put("isHidden", override.isHidden),
            )
        }

        val blockedApps = JSONArray()
        blocklistRepository.blockedApps.first().forEach { app ->
            blockedApps.put(
                JSONObject()
                    .put("packageName", app.packageName)
                    .put("delaySeconds", app.delaySeconds)
                    .put("dailyOpenLimit", app.dailyOpenLimit ?: JSONObject.NULL)
                    .put("dailyTimeBudgetMinutes", app.dailyTimeBudgetMinutes ?: JSONObject.NULL)
                    .put("requireIntention", app.requireIntention),
            )
        }

        val mutedApps = JSONArray()
        notificationMuteRepository.mutedApps.first().forEach { app ->
            mutedApps.put(JSONObject().put("packageName", app.packageName))
        }

        val focusSchedules = JSONArray()
        focusScheduleRepository.schedules.first().forEach { schedule ->
            focusSchedules.put(
                JSONObject()
                    .put("startHour", schedule.startHour)
                    .put("endHour", schedule.endHour)
                    .put("daysMask", schedule.daysMask)
                    .put("enabled", schedule.enabled),
            )
        }

        val workProfileFavorites = JSONArray()
        workProfileRepository.favorites.first().forEach { fav ->
            workProfileFavorites.put(JSONObject().put("appId", fav.appId).put("position", fav.position))
        }

        val workProfileAllowedApps = JSONArray()
        workProfileRepository.allowedApps.first().forEach { entry ->
            workProfileAllowedApps.put(JSONObject().put("appId", entry.appId))
        }

        return JSONObject()
            .put("version", 1)
            .put("settings", settings)
            .put("favorites", favorites)
            .put("hiddenApps", hiddenApps)
            .put("blockedApps", blockedApps)
            .put("mutedApps", mutedApps)
            .put("focusSchedules", focusSchedules)
            .put("workProfileFavorites", workProfileFavorites)
            .put("workProfileAllowedApps", workProfileAllowedApps)
    }

    private suspend fun applyBackupJson(root: JSONObject) {
        root.optJSONObject("settings")?.let { settings ->
            settings.optStringOrNull("themeMode")?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                ?.let { settingsRepository.setThemeMode(it) }
            settings.optStringOrNull("fontFamily")?.let { name -> runCatching { AppFontFamily.valueOf(name) }.getOrNull() }
                ?.let { settingsRepository.setFontFamily(it) }
            settings.optStringOrNull("fontSize")?.let { name -> runCatching { FontSize.valueOf(name) }.getOrNull() }
                ?.let { settingsRepository.setFontSize(it) }
            settings.optStringOrNull("alignment")?.let { name -> runCatching { HomeAlignment.valueOf(name) }.getOrNull() }
                ?.let { settingsRepository.setAlignment(it) }
            settingsRepository.setShowScreenTime(settings.optBoolean("showScreenTime", false))
            settingsRepository.setShowMutedCount(settings.optBoolean("showMutedCount", false))
            settingsRepository.setGrayscaleEnabled(settings.optBoolean("grayscaleEnabled", false))
            settingsRepository.setFocusAutomationEnabled(settings.optBoolean("focusAutomationEnabled", false))
            settingsRepository.setShowFocusStatus(settings.optBoolean("showFocusStatus", false))
            settingsRepository.setNotificationDigestEnabled(settings.optBoolean("notificationDigestEnabled", false))
            settingsRepository.setDailyGoalMinutes(
                if (settings.has("dailyGoalMinutes") && !settings.isNull("dailyGoalMinutes")) settings.getInt("dailyGoalMinutes") else null,
            )
        }

        database.withTransaction {
            favoritesRepository.replaceAll(
                root.optJSONArray("favorites")?.mapObjects { o ->
                    FavoriteEntity(appId = o.getString("appId"), position = o.getInt("position"))
                } ?: emptyList(),
            )
            appOverridesRepository.replaceAll(
                root.optJSONArray("hiddenApps")?.mapObjects { o ->
                    AppOverrideEntity(
                        appId = o.getString("appId"),
                        customLabel = o.optStringOrNull("customLabel"),
                        isHidden = o.optBoolean("isHidden", false),
                    )
                } ?: emptyList(),
            )
            blocklistRepository.replaceAll(
                root.optJSONArray("blockedApps")?.mapObjects { o ->
                    BlockedAppEntity(
                        packageName = o.getString("packageName"),
                        delaySeconds = o.optInt("delaySeconds", 10),
                        dailyOpenLimit = if (o.has("dailyOpenLimit") && !o.isNull("dailyOpenLimit")) o.getInt("dailyOpenLimit") else null,
                        dailyTimeBudgetMinutes = if (o.has("dailyTimeBudgetMinutes") && !o.isNull("dailyTimeBudgetMinutes")) o.getInt("dailyTimeBudgetMinutes") else null,
                        requireIntention = o.optBoolean("requireIntention", false),
                    )
                } ?: emptyList(),
            )
            notificationMuteRepository.replaceMutedApps(
                root.optJSONArray("mutedApps")?.mapObjects { o -> MutedAppEntity(packageName = o.getString("packageName")) } ?: emptyList(),
            )
            focusScheduleRepository.replaceAll(
                root.optJSONArray("focusSchedules")?.mapObjects { o ->
                    FocusScheduleEntity(
                        startHour = o.optInt("startHour", 9),
                        endHour = o.optInt("endHour", 17),
                        daysMask = o.optInt("daysMask", 0b0011111),
                        enabled = o.optBoolean("enabled", true),
                    )
                } ?: emptyList(),
            )
            workProfileRepository.replaceAllFavorites(
                root.optJSONArray("workProfileFavorites")?.mapObjects { o ->
                    WorkProfileFavoriteEntity(appId = o.getString("appId"), position = o.getInt("position"))
                } ?: emptyList(),
            )
            workProfileRepository.replaceAllAllowedApps(
                root.optJSONArray("workProfileAllowedApps")?.mapObjects { o ->
                    WorkProfileAllowedAppEntity(appId = o.getString("appId"))
                } ?: emptyList(),
            )
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
