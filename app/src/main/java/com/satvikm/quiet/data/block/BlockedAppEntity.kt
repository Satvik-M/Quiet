package com.satvikm.quiet.data.block

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val delaySeconds: Int = 10,
    /** Null means no daily cap on how many times "Continue" can be used. */
    val dailyOpenLimit: Int? = null,
    /** Null means no daily cap on foreground time; once today's usage reaches this many minutes, "Continue" is hidden until midnight. */
    val dailyTimeBudgetMinutes: Int? = null,
    /** When true, the friction screen requires a typed one-line reason before "Continue" is enabled. */
    val requireIntention: Boolean = false,
    /** Non-null while a removal cooldown is running — the entry still fully applies until this passes, so removing friction can't be done in one impulsive tap. See [BlocklistRepository.requestRemoval]. */
    val pendingRemovalAtMillis: Long? = null,
)
