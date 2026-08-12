package com.satvikm.quiet.data.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val appId: String,
    val position: Int,
)
