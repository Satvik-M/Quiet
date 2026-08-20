package com.satvikm.quiet.data.block

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A blocked app is exempt from re-triggering friction until [graceUntilMillis] (wall-clock) — set when the user taps Continue. */
@Entity(tableName = "friction_grace")
data class GraceEntity(
    @PrimaryKey val packageName: String,
    val graceUntilMillis: Long,
)
