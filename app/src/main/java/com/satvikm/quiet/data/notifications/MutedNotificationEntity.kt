package com.satvikm.quiet.data.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per notification cancelled by [com.satvikm.quiet.service.NotificationFilterService]
 * — powers the "muted today" count and the digest view. [title]/[text] are only populated
 * when the user has opted into [com.satvikm.quiet.data.settings.SettingsRepository.notificationDigestEnabled];
 * otherwise this row is content-free and the digest falls back to showing just the app and time.
 */
@Entity(tableName = "muted_notifications")
data class MutedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampMillis: Long,
    val title: String? = null,
    val text: String? = null,
)
