package com.satvikm.quiet.data.block

import com.satvikm.quiet.data.usage.UsageRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val dao: BlockedAppDao,
    private val graceDao: GraceDao,
    private val usageRepository: UsageRepository,
) {
    val blockedApps: Flow<List<BlockedAppEntity>> = dao.observeAll()

    /** Persists a fresh grace window for [packageName], so it survives a service restart. */
    suspend fun grantGrace(packageName: String) {
        val now = System.currentTimeMillis()
        graceDao.deleteExpired(now)
        graceDao.upsert(GraceEntity(packageName, now + GRACE_DURATION_MS))
    }

    /** Non-expired grace windows, for the accessibility service to seed its in-memory cache on (re)connect. */
    suspend fun activeGrace(): Map<String, Long> =
        graceDao.getAllActive(System.currentTimeMillis()).associate { it.packageName to it.graceUntilMillis }

    suspend fun isBlocked(packageName: String): Boolean = dao.get(packageName) != null

    suspend fun get(packageName: String): BlockedAppEntity? = dao.get(packageName)

    suspend fun setBlocked(
        packageName: String,
        delaySeconds: Int = 10,
        dailyOpenLimit: Int? = null,
        dailyTimeBudgetMinutes: Int? = null,
        requireIntention: Boolean = false,
    ) {
        dao.upsert(BlockedAppEntity(packageName, delaySeconds, dailyOpenLimit, dailyTimeBudgetMinutes, requireIntention))
    }

    suspend fun unblock(packageName: String) {
        dao.delete(packageName)
    }

    /** Replaces the entire friction list with [entities] — used by backup restore. */
    suspend fun replaceAll(entities: List<BlockedAppEntity>) {
        dao.deleteAll()
        entities.forEach { dao.upsert(it) }
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

    /** True if this app has no daily time budget, or today's foreground time is still under it. */
    suspend fun withinTimeBudget(entity: BlockedAppEntity): Boolean {
        val budgetMinutes = entity.dailyTimeBudgetMinutes ?: return true
        val usedMillis = usageRepository.today().perApp
            .firstOrNull { it.packageName == entity.packageName }
            ?.foregroundMillis ?: 0L
        return usedMillis < budgetMinutes * 60_000L
    }

    companion object {
        const val GRACE_DURATION_MS = 2 * 60_000L
    }
}
