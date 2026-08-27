package com.satvikm.quiet.di

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.satvikm.quiet.data.apps.AppDao
import com.satvikm.quiet.data.apps.AppDatabase
import com.satvikm.quiet.data.apps.AppOverrideDao
import com.satvikm.quiet.data.block.BlockedAppDao
import com.satvikm.quiet.data.block.GraceDao
import com.satvikm.quiet.data.favorites.FavoriteDao
import com.satvikm.quiet.data.focus.FocusAutoMuteDao
import com.satvikm.quiet.data.focus.FocusScheduleDao
import com.satvikm.quiet.data.notifications.MutedAppDao
import com.satvikm.quiet.data.workprofile.WorkProfileAllowedAppDao
import com.satvikm.quiet.data.workprofile.WorkProfileFavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Adds Work Mode's two tables. Explicit (non-destructive) because this is the one version step
 * this app has a real Migration for — see the destructive-fallback comment below for why other
 * steps still rely on that instead.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `work_profile_favorites` (`appId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`appId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `work_profile_allowed_apps` (`appId` TEXT NOT NULL, PRIMARY KEY(`appId`))",
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLauncherApps(@ApplicationContext context: Context): LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    @Provides
    @Singleton
    fun provideUserManager(@ApplicationContext context: Context): UserManager =
        context.getSystemService(UserManager::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "quiet.db")
            // Pre-release: avoids writing real Migrations for now. Still
            // requires bumping @Database's version on every schema change —
            // Room treats a same-version identity mismatch as a hard error,
            // not something this falls back for. Revisit before M14.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addMigrations(MIGRATION_12_13)
            .build()

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideAppOverrideDao(database: AppDatabase): AppOverrideDao = database.appOverrideDao()

    @Provides
    fun provideBlockedAppDao(database: AppDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideMutedAppDao(database: AppDatabase): MutedAppDao = database.mutedAppDao()

    @Provides
    fun provideFocusScheduleDao(database: AppDatabase): FocusScheduleDao = database.focusScheduleDao()

    @Provides
    fun provideFocusAutoMuteDao(database: AppDatabase): FocusAutoMuteDao = database.focusAutoMuteDao()

    @Provides
    fun provideGraceDao(database: AppDatabase): GraceDao = database.graceDao()

    @Provides
    fun provideWorkProfileFavoriteDao(database: AppDatabase): WorkProfileFavoriteDao = database.workProfileFavoriteDao()

    @Provides
    fun provideWorkProfileAllowedAppDao(database: AppDatabase): WorkProfileAllowedAppDao = database.workProfileAllowedAppDao()
}
