package com.satvikm.quiet.di

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.room.Room
import com.satvikm.quiet.data.apps.AppDao
import com.satvikm.quiet.data.apps.AppDatabase
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
        Room.databaseBuilder(context, AppDatabase::class.java, "quiet.db").build()

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()
}
