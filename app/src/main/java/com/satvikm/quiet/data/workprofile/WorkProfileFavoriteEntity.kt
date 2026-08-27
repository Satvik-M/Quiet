package com.satvikm.quiet.data.workprofile

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Work Mode's own home-screen favorites list — separate from [com.satvikm.quiet.data.favorites.FavoriteEntity]. */
@Entity(tableName = "work_profile_favorites")
data class WorkProfileFavoriteEntity(
    @PrimaryKey val appId: String,
    val position: Int,
)
