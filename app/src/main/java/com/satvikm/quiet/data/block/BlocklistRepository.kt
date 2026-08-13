package com.satvikm.quiet.data.block

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val dao: BlockedAppDao,
) {
    val blockedApps: Flow<List<BlockedAppEntity>> = dao.observeAll()

    suspend fun isBlocked(packageName: String): Boolean = dao.get(packageName) != null

    suspend fun get(packageName: String): BlockedAppEntity? = dao.get(packageName)

    suspend fun setBlocked(packageName: String, delaySeconds: Int = 10, dailyOpenLimit: Int? = null) {
        dao.upsert(BlockedAppEntity(packageName, delaySeconds, dailyOpenLimit))
    }

    suspend fun unblock(packageName: String) {
        dao.delete(packageName)
    }

    suspend fun recordOpen(packageName: String) {
        dao.logOpen(AppOpenEntity(packageName = packageName, timestampMillis = System.currentTimeMillis()))
    }

    /** True if this app has no daily cap, or today's "Continue" count is still under it. */
    suspend fun canContinue(entity: BlockedAppEntity): Boolean {
        val limit = entity.dailyOpenLimit ?: return true
        val startOfDay = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.countOpensSince(entity.packageName, startOfDay) < limit
    }
}
