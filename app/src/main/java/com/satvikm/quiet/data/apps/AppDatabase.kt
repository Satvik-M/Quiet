package com.satvikm.quiet.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
