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

    /** Replaces every schedule with [entities] — used by backup restore. */
    suspend fun replaceAll(entities: List<FocusScheduleEntity>) {
        dao.deleteAll()
        entities.forEach { dao.upsert(it) }
    }

    /** Whether any enabled schedule covers this instant — used by the friction screen, which needs this "live" with no UI open. */
    suspend fun isFocusActiveNow(): Boolean = isActiveAt(dao.getAll(), ZonedDateTime.now())

    companion object {
        /** Pure version of [isFocusActiveNow], reusable against an already-loaded schedule list (e.g. for a reactive UI flow). */
        fun isActiveAt(schedules: List<FocusScheduleEntity>, now: ZonedDateTime): Boolean {
            val dayBit = 1 shl (now.dayOfWeek.value - 1)
            val hour = now.hour
            return schedules.any { schedule ->
                schedule.enabled && (schedule.daysMask and dayBit) != 0 && hourInRange(hour, schedule.startHour, schedule.endHour)
            }
        }

        private fun hourInRange(hour: Int, start: Int, end: Int): Boolean =
            if (start <= end) hour in start until end else hour >= start || hour < end
    }
}
