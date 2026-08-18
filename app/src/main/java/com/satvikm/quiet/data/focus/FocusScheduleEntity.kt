package com.satvikm.quiet.data.focus

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [daysMask] bit (dayOfWeek.value - 1) — bit 0 = Monday .. bit 6 = Sunday (java.time.DayOfWeek). */
@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startHour: Int = 9,
    val endHour: Int = 17,
    val daysMask: Int = 0b0011111,
    val enabled: Boolean = true,
)
