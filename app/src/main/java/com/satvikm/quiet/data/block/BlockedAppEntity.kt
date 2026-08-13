package com.satvikm.quiet.data.block

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val delaySeconds: Int = 10,
    /** Null means no daily cap on how many times "Continue" can be used. */
    val dailyOpenLimit: Int? = null,
)
