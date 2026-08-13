package com.satvikm.quiet.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase
import com.satvikm.quiet.data.block.AppOpenEntity
import com.satvikm.quiet.data.block.BlockedAppDao
import com.satvikm.quiet.data.block.BlockedAppEntity
import com.satvikm.quiet.data.favorites.FavoriteDao
import com.satvikm.quiet.data.favorites.FavoriteEntity

@Database(
    entities = [
        AppEntity::class,
        FavoriteEntity::class,
        AppOverrideEntity::class,
        BlockedAppEntity::class,
        AppOpenEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun appOverrideDao(): AppOverrideDao
    abstract fun blockedAppDao(): BlockedAppDao
}
