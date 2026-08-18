package com.satvikm.quiet.data.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per notification cancelled by [com.satvikm.quiet.service.NotificationFilterService] — powers the "muted today" count. */
@Entity(tableName = "muted_notifications")
data class MutedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampMillis: Long,
)
