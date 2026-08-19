package com.satvikm.quiet.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
    }

    val themeMode: Flow<ThemeMode> = enumFlow(Keys.THEME_MODE, ThemeMode.SYSTEM, ThemeMode::valueOf)
    val fontFamily: Flow<AppFontFamily> = enumFlow(Keys.FONT_FAMILY, AppFontFamily.SANS, AppFontFamily::valueOf)
    val fontSize: Flow<FontSize> = enumFlow(Keys.FONT_SIZE, FontSize.MEDIUM, FontSize::valueOf)
    val alignment: Flow<HomeAlignment> = enumFlow(Keys.ALIGNMENT, HomeAlignment.LEFT, HomeAlignment::valueOf)
    val showScreenTime: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.SHOW_SCREEN_TIME] ?: false }
    val showMutedCount: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.SHOW_MUTED_COUNT] ?: false }
    val grayscaleEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.GRAYSCALE_ENABLED] ?: false }
    val focusAutomationEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { it[Keys.FOCUS_AUTOMATION_ENABLED] ?: false }

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
