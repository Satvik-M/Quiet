package com.satvikm.quiet.di

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.room.Room
import com.satvikm.quiet.data.apps.AppDao
import com.satvikm.quiet.data.apps.AppDatabase
import com.satvikm.quiet.data.apps.AppOverrideDao
import com.satvikm.quiet.data.block.BlockedAppDao
import com.satvikm.quiet.data.favorites.FavoriteDao
import com.satvikm.quiet.data.focus.FocusAutoMuteDao
import com.satvikm.quiet.data.focus.FocusScheduleDao
import com.satvikm.quiet.data.notifications.MutedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
}
