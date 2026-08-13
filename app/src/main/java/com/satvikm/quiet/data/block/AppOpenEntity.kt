package com.satvikm.quiet.data.block

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per "Continue" tap in the friction screen — used to enforce daily open limits. */
@Entity(tableName = "app_opens")
data class AppOpenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampMillis: Long,
)
