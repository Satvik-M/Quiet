package com.satvikm.quiet.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase
import com.satvikm.quiet.data.block.AppOpenEntity
import com.satvikm.quiet.data.block.BlockedAppDao
import com.satvikm.quiet.data.block.BlockedAppEntity
import com.satvikm.quiet.data.block.GraceDao
import com.satvikm.quiet.data.block.GraceEntity
import com.satvikm.quiet.data.favorites.FavoriteDao
import com.satvikm.quiet.data.favorites.FavoriteEntity
import com.satvikm.quiet.data.focus.FocusAutoMuteDao
import com.satvikm.quiet.data.focus.FocusAutoMutedAppEntity
import com.satvikm.quiet.data.focus.FocusScheduleDao
import com.satvikm.quiet.data.focus.FocusScheduleEntity
import com.satvikm.quiet.data.notifications.MutedAppDao
import com.satvikm.quiet.data.notifications.MutedAppEntity
import com.satvikm.quiet.data.notifications.MutedNotificationEntity

@Database(
    entities = [
        AppEntity::class,
        FavoriteEntity::class,
        AppOverrideEntity::class,
        BlockedAppEntity::class,
        AppOpenEntity::class,
        MutedAppEntity::class,
        MutedNotificationEntity::class,
        FocusScheduleEntity::class,
        FocusAutoMutedAppEntity::class,
        GraceEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun appOverrideDao(): AppOverrideDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun mutedAppDao(): MutedAppDao
    abstract fun focusScheduleDao(): FocusScheduleDao
    abstract fun focusAutoMuteDao(): FocusAutoMuteDao
    abstract fun graceDao(): GraceDao
}
