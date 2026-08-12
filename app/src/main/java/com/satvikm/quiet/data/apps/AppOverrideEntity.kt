package com.satvikm.quiet.data.apps

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User customizations for an app that must survive the app-list cache being rebuilt from the system. */
@Entity(tableName = "app_overrides")
data class AppOverrideEntity(
    @PrimaryKey val appId: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
)
