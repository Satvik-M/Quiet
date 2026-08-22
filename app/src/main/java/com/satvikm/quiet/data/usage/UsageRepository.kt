package com.satvikm.quiet.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.satvikm.quiet.data.apps.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsage(val packageName: String, val foregroundMillis: Long)

data class DailyUsage(
    val totalMillis: Long,
    val unlockCount: Int,
    val perApp: List<AppUsage>,
)

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
) {
    private val appOps: AppOpsManager
        get() = context.getSystemService(AppOpsManager::class.java)

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(UsageStatsManager::class.java)

    fun isUsageAccessGranted(): Boolean {
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Per-app foreground time and unlock count for [date] (all of it, if [date] is in the past), computed from raw events (not the coarse aggregate buckets). */
    suspend fun usageForDate(date: LocalDate): DailyUsage = withContext(Dispatchers.Default) {
        if (!isUsageAccessGranted()) return@withContext DailyUsage(0L, 0, emptyList())

        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val queryEnd = minOf(endOfDay, System.currentTimeMillis())
        val keyguardHiddenEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            UsageEvents.Event.KEYGUARD_HIDDEN
        } else {
            null
        }

        val foregroundSince = mutableMapOf<String, Long>()
        val perApp = mutableMapOf<String, Long>()
        var unlockCount = 0

        val events = usageStatsManager.queryEvents(startOfDay, queryEnd)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> foregroundSince[event.packageName] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = foregroundSince.remove(event.packageName)
                    if (start != null && event.timeStamp > start) {
                        perApp[event.packageName] = (perApp[event.packageName] ?: 0L) + (event.timeStamp - start)
                    }
                }
                keyguardHiddenEventType -> unlockCount++
            }
        }
        // Anything still foregrounded (e.g. this app, right now, for today) counts up to queryEnd.
        for ((packageName, start) in foregroundSince) {
            perApp[packageName] = (perApp[packageName] ?: 0L) + (queryEnd - start)
        }

        // Raw events include system UI components (status bar, keyguard, the
        // system launcher's brief flashes during app switches, etc.) that aren't
        // apps the user thinks of as "using" — only count packages with a real
        // launcher entry, in both the per-app breakdown and the total.
        val launchablePackages = appRepository.apps.first().map { it.packageName }.toSet()
        val filteredPerApp = perApp.filterKeys { it in launchablePackages }

        DailyUsage(
            totalMillis = filteredPerApp.values.sum(),
            unlockCount = unlockCount,
            perApp = filteredPerApp.map { (packageName, millis) -> AppUsage(packageName, millis) }
                .sortedByDescending { it.foregroundMillis },
        )
    }

    suspend fun today(): DailyUsage = usageForDate(LocalDate.now(ZoneId.systemDefault()))

    /** Oldest-to-newest daily totals for the last [count] days, including today. */
    suspend fun lastDays(count: Int): List<Pair<LocalDate, DailyUsage>> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return (count - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            date to usageForDate(date)
        }
    }

    /**
     * Consecutive completed days (ending yesterday) whose total foreground
     * time stayed within [goalMinutes] — today doesn't count yet since it
     * isn't over. Capped at [maxDays]: the OS doesn't reliably retain raw
     * usage events much further back than that, so an uncapped walk could
     * silently read missing history as "goal met" and inflate the streak.
     */
    suspend fun streak(goalMinutes: Int, maxDays: Int = 14): Int {
        val goalMillis = goalMinutes * 60_000L
        val today = LocalDate.now(ZoneId.systemDefault())
        var count = 0
        for (offset in 1..maxDays) {
            if (usageForDate(today.minusDays(offset.toLong())).totalMillis > goalMillis) break
            count++
        }
        return count
    }

    companion object {
        /** Per-app totals across [days] of [lastDays] output, summed and ranked highest-first — for a "most used this week/month" view rather than a single day's breakdown. */
        fun topApps(days: List<Pair<LocalDate, DailyUsage>>, limit: Int = 8): List<AppUsage> =
            days.flatMap { it.second.perApp }
                .groupBy { it.packageName }
                .map { (packageName, usages) -> AppUsage(packageName, usages.sumOf { it.foregroundMillis }) }
                .sortedByDescending { it.foregroundMillis }
                .take(limit)
    }
}
