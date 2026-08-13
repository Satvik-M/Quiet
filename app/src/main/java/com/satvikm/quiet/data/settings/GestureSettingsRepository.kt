package com.satvikm.quiet.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gestureSettingsDataStore by preferencesDataStore(name = "gesture_settings")

enum class GestureSlot(val key: Preferences.Key<String>) {
    SWIPE_LEFT(stringPreferencesKey("swipe_left_app_id")),
    SWIPE_RIGHT(stringPreferencesKey("swipe_right_app_id")),
}

@Singleton
class GestureSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun appIdFor(slot: GestureSlot): Flow<String?> =
        context.gestureSettingsDataStore.data.map { it[slot.key] }

    suspend fun setAppFor(slot: GestureSlot, appId: String?) {
        context.gestureSettingsDataStore.edit { prefs ->
            if (appId == null) prefs.remove(slot.key) else prefs[slot.key] = appId
        }
    }
}
