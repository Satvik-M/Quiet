package com.satvikm.quiet.data.notifications

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationMuteRepository @Inject constructor(
    private val dao: MutedAppDao,
) {
    val mutedApps: Flow<List<MutedAppEntity>> = dao.observeAll()

    suspend fun isMuted(packageName: String): Boolean = dao.get(packageName) != null

    suspend fun mute(packageName: String) {
        dao.upsert(MutedAppEntity(packageName))
    }

    suspend fun unmute(packageName: String) {
        dao.delete(packageName)
    }

    suspend fun recordMuted(packageName: String) {
        dao.logMuted(MutedNotificationEntity(packageName = packageName, timestampMillis = System.currentTimeMillis()))
    }

    suspend fun mutedCountToday(): Int {
        val startOfDay = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.countMutedSince(startOfDay)
    }
}
