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

    /** Per-app foreground time and unlock count since local midnight, computed from raw events (not the coarse aggregate buckets). */
    suspend fun today(): DailyUsage = withContext(Dispatchers.Default) {
        if (!isUsageAccessGranted()) return@withContext DailyUsage(0L, 0, emptyList())

        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val keyguardHiddenEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            UsageEvents.Event.KEYGUARD_HIDDEN
        } else {
            null
        }

        val foregroundSince = mutableMapOf<String, Long>()
        val perApp = mutableMapOf<String, Long>()
        var unlockCount = 0

        val events = usageStatsManager.queryEvents(startOfDay, now)
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
        // Anything still foregrounded (e.g. this app, right now) counts up to "now".
        for ((packageName, start) in foregroundSince) {
            perApp[packageName] = (perApp[packageName] ?: 0L) + (now - start)
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
}
