package com.satvikm.quiet.data.focus

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusAutoMuteRepository @Inject constructor(
    private val dao: FocusAutoMuteDao,
) {
    val autoMuted: Flow<List<FocusAutoMutedAppEntity>> = dao.observeAll()

    suspend fun setAutoMuted(packageNames: Set<String>) {
        dao.replaceAll(packageNames)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
