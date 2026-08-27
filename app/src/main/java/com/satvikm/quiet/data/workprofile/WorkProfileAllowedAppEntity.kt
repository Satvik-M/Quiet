package com.satvikm.quiet.data.workprofile

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Membership row: an app's presence in this table means it's part of Work Mode's curated allowlist. */
@Entity(tableName = "work_profile_allowed_apps")
data class WorkProfileAllowedAppEntity(
    @PrimaryKey val appId: String,
)
