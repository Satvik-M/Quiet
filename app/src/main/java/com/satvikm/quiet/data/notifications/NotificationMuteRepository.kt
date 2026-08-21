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

    /** Replaces the entire muted-apps list with [entities] — used by backup restore. Doesn't touch the digest log. */
    suspend fun replaceMutedApps(entities: List<MutedAppEntity>) {
        dao.deleteAllMuted()
        entities.forEach { dao.upsert(it) }
    }

    /**
     * Logs a cancelled notification for the "muted today" count and the digest view, then
     * prunes anything older than [DIGEST_RETENTION_DAYS] so the log table doesn't grow forever.
     * [title]/[text] should be null unless the user opted into content capture — the caller
     * decides that, this just persists whatever it's given.
     */
    suspend fun recordMuted(packageName: String, title: String? = null, text: String? = null) {
        dao.logMuted(
            MutedNotificationEntity(
                packageName = packageName,
                timestampMillis = System.currentTimeMillis(),
                title = title,
                text = text,
            ),
        )
        dao.deleteOlderThan(System.currentTimeMillis() - DIGEST_RETENTION_DAYS * 24 * 60 * 60_000L)
    }

    suspend fun mutedCountToday(): Int = dao.countMutedSince(startOfToday())

    /** Newest-first log of cancelled notifications from today, for the digest screen. */
    suspend fun digestToday(): List<MutedNotificationEntity> = dao.getSince(startOfToday())

    private fun startOfToday(): Long = LocalDate.now(ZoneId.systemDefault())
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    companion object {
        private const val DIGEST_RETENTION_DAYS = 7
    }
}
