package com.satvikm.quiet.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppFontFamily { SANS, MONOSPACE }
enum class FontSize(val scale: Float) { SMALL(0.85f), MEDIUM(1f), LARGE(1.25f) }
enum class HomeAlignment { LEFT, CENTER }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val ALIGNMENT = stringPreferencesKey("alignment")
        val SHOW_SCREEN_TIME = booleanPreferencesKey("show_screen_time")
        val SHOW_MUTED_COUNT = booleanPreferencesKey("show_muted_count")
        val GRAYSCALE_ENABLED = booleanPreferencesKey("grayscale_enabled")
        val FOCUS_AUTOMATION_ENABLED = booleanPreferencesKey("focus_automation_enabled")
        val SHOW_FOCUS_STATUS = booleanPreferencesKey("show_focus_status")
        val MANUAL_FOCUS_ACTIVE = booleanPreferencesKey("manual_focus_active")
        val MANUAL_FOCUS_ENDS_AT = longPreferencesKey("manual_focus_ends_at")
        val MANUAL_FOCUS_LOCKED = booleanPreferencesKey("manual_focus_locked")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NOTIFICATION_DIGEST_ENABLED = booleanPreferencesKey("notification_digest_enabled")
        val FOCUS_RECAP_ENABLED = booleanPreferencesKey("focus_recap_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val themeMode: Flow<ThemeMode> = enumFlow(Keys.THEME_MODE, ThemeMode.SYSTEM, ThemeMode::valueOf)
    val fontFamily: Flow<AppFontFamily> = enumFlow(Keys.FONT_FAMILY, AppFontFamily.SANS, AppFontFamily::valueOf)
    val fontSize: Flow<FontSize> = enumFlow(Keys.FONT_SIZE, FontSize.MEDIUM, FontSize::valueOf)
    val alignment: Flow<HomeAlignment> = enumFlow(Keys.ALIGNMENT, HomeAlignment.LEFT, HomeAlignment::valueOf)
    val showScreenTime: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.SHOW_SCREEN_TIME] ?: false }
    val showMutedCount: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.SHOW_MUTED_COUNT] ?: false }
    val grayscaleEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.GRAYSCALE_ENABLED] ?: false }
    val focusAutomationEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.FOCUS_AUTOMATION_ENABLED] ?: false }
    val showFocusStatus: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.SHOW_FOCUS_STATUS] ?: false }
    /** Ad-hoc "Focus now" override — applies focus effects immediately regardless of any schedule, toggled from the home screen or the Quick Settings tile. */
    val manualFocusActive: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.MANUAL_FOCUS_ACTIVE] ?: false }
    /** Wall-clock time the current manual focus session ends; null means it was started with no timer (ends only when toggled off). */
    val manualFocusEndsAtMillis: Flow<Long?> = context.appSettingsDataStore.data.map { it[Keys.MANUAL_FOCUS_ENDS_AT] }
    /** Whether the current manual focus session refuses to be ended early — see [endManualFocusIfAllowed]. */
    val manualFocusLocked: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.MANUAL_FOCUS_LOCKED] ?: false }
    /** Null means no daily screen-time goal is set. */
    val dailyGoalMinutes: Flow<Int?> = context.appSettingsDataStore.data.map { it[Keys.DAILY_GOAL_MINUTES] }
    /** When on, [com.satvikm.quiet.service.NotificationFilterService] stores the title/text of muted notifications so the digest screen can show what was suppressed, not just how many. */
    val notificationDigestEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.NOTIFICATION_DIGEST_ENABLED] ?: false }
    /** Whether [com.satvikm.quiet.data.focus.FocusModeOrchestrator] posts a summary notification when a focus session ends. */
    val focusRecapEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.FOCUS_RECAP_ENABLED] ?: false }
    val onboardingCompleted: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) = set(Keys.THEME_MODE, mode.name)
    suspend fun setFontFamily(family: AppFontFamily) = set(Keys.FONT_FAMILY, family.name)
    suspend fun setFontSize(size: FontSize) = set(Keys.FONT_SIZE, size.name)
    suspend fun setAlignment(alignment: HomeAlignment) = set(Keys.ALIGNMENT, alignment.name)
    suspend fun setShowScreenTime(show: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.SHOW_SCREEN_TIME] = show }
    }
    suspend fun setShowMutedCount(show: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.SHOW_MUTED_COUNT] = show }
    }
    suspend fun setGrayscaleEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.GRAYSCALE_ENABLED] = enabled }
    }
    suspend fun setFocusAutomationEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.FOCUS_AUTOMATION_ENABLED] = enabled }
    }
    suspend fun setShowFocusStatus(show: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.SHOW_FOCUS_STATUS] = show }
    }
    suspend fun setManualFocusActive(active: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.MANUAL_FOCUS_ACTIVE] = active }
    }
    /** Starts an ad-hoc focus session. [durationMinutes] null means no timer — it only ends when [endManualFocusIfAllowed] is called and [locked] is false. Locking with no duration is refused by the caller (see [com.satvikm.quiet.ui.home.HomeScreen]) since it would leave no way to ever end the session. */
    suspend fun startManualFocus(durationMinutes: Int?, locked: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.MANUAL_FOCUS_ACTIVE] = true
            if (durationMinutes != null) {
                prefs[Keys.MANUAL_FOCUS_ENDS_AT] = System.currentTimeMillis() + durationMinutes * 60_000L
            } else {
                prefs.remove(Keys.MANUAL_FOCUS_ENDS_AT)
            }
            prefs[Keys.MANUAL_FOCUS_LOCKED] = locked
        }
    }
    /** Ends the current manual focus session unless it's locked and its timer hasn't run out yet. Returns whether it actually ended. */
    suspend fun endManualFocusIfAllowed(): Boolean {
        val prefs = context.appSettingsDataStore.data.first()
        val locked = prefs[Keys.MANUAL_FOCUS_LOCKED] ?: false
        val endsAt = prefs[Keys.MANUAL_FOCUS_ENDS_AT]
        if (locked && endsAt != null && System.currentTimeMillis() < endsAt) return false
        clearManualFocus()
        return true
    }
    /** If a timed manual focus session has run past its end time, clears it — called from the background poll loop so "End focus" stops showing once the timer is up even with no UI open. */
    suspend fun clearExpiredManualFocus() {
        val prefs = context.appSettingsDataStore.data.first()
        val active = prefs[Keys.MANUAL_FOCUS_ACTIVE] ?: false
        val endsAt = prefs[Keys.MANUAL_FOCUS_ENDS_AT]
        if (active && endsAt != null && System.currentTimeMillis() >= endsAt) clearManualFocus()
    }
    private suspend fun clearManualFocus() {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.MANUAL_FOCUS_ACTIVE] = false
            prefs.remove(Keys.MANUAL_FOCUS_ENDS_AT)
            prefs.remove(Keys.MANUAL_FOCUS_LOCKED)
        }
    }
    suspend fun setDailyGoalMinutes(minutes: Int?) {
        context.appSettingsDataStore.edit {
            if (minutes == null) it.remove(Keys.DAILY_GOAL_MINUTES) else it[Keys.DAILY_GOAL_MINUTES] = minutes
        }
    }
    suspend fun setNotificationDigestEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.NOTIFICATION_DIGEST_ENABLED] = enabled }
    }
    suspend fun setFocusRecapEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.FOCUS_RECAP_ENABLED] = enabled }
    }
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appSettingsDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private fun <T : Enum<T>> enumFlow(
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        default: T,
        parse: (String) -> T,
    ): Flow<T> = context.appSettingsDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { parse(it) }.getOrNull() } ?: default
    }

    private suspend fun set(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.appSettingsDataStore.edit { it[key] = value }
    }
}
