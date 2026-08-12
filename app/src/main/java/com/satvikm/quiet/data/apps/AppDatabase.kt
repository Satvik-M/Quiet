package com.satvikm.quiet.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase
import com.satvikm.quiet.data.favorites.FavoriteDao
import com.satvikm.quiet.data.favorites.FavoriteEntity

@Database(entities = [AppEntity::class, FavoriteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun favoriteDao(): FavoriteDao
}
