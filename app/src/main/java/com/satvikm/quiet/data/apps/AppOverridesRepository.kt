package com.satvikm.quiet.data.apps

import com.satvikm.quiet.domain.model.LaunchableApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOverridesRepository @Inject constructor(
    private val dao: AppOverrideDao,
) {
    val overrides: Flow<List<AppOverrideEntity>> = dao.observeAll()

    suspend fun setHidden(app: LaunchableApp, hidden: Boolean) {
        applyOrClear(app.id) { it.copy(isHidden = hidden) }
    }

    suspend fun setCustomLabel(app: LaunchableApp, label: String?) {
        val trimmed = label?.trim()?.takeIf { it.isNotEmpty() }
        applyOrClear(app.id) { it.copy(customLabel = trimmed) }
    }

    private suspend fun applyOrClear(appId: String, update: (AppOverrideEntity) -> AppOverrideEntity) {
        val current = dao.get(appId) ?: AppOverrideEntity(appId = appId)
        val updated = update(current)
        if (updated.customLabel == null && !updated.isHidden) {
            dao.delete(appId)
        } else {
            dao.upsert(updated)
        }
    }
}
