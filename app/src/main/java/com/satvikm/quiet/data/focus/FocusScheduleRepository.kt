package com.satvikm.quiet.data.focus

import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusScheduleRepository @Inject constructor(
    private val dao: FocusScheduleDao,
) {
    val schedules: Flow<List<FocusScheduleEntity>> = dao.observeAll()

    suspend fun addDefault() {
        dao.upsert(FocusScheduleEntity())
    }

    suspend fun update(entity: FocusScheduleEntity) {
        dao.upsert(entity)
    }

    suspend fun delete(entity: FocusScheduleEntity) {
        dao.delete(entity)
    }

    /** Whether any enabled schedule covers this instant — used by the friction screen, which needs this "live" with no UI open. */
    suspend fun isFocusActiveNow(): Boolean {
        val now = ZonedDateTime.now()
        val dayBit = 1 shl (now.dayOfWeek.value - 1)
        val hour = now.hour
        return dao.getAll().any { schedule ->
            schedule.enabled && (schedule.daysMask and dayBit) != 0 && hourInRange(hour, schedule.startHour, schedule.endHour)
        }
    }

    private fun hourInRange(hour: Int, start: Int, end: Int): Boolean =
        if (start <= end) hour in start until end else hour >= start || hour < end
}
