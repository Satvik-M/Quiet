package com.satvikm.quiet.data.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MutedAppDao {

    @Query("SELECT * FROM muted_apps")
    abstract fun observeAll(): Flow<List<MutedAppEntity>>

    @Query("SELECT * FROM muted_apps WHERE packageName = :packageName")
    abstract suspend fun get(packageName: String): MutedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: MutedAppEntity)

    @Query("DELETE FROM muted_apps WHERE packageName = :packageName")
    abstract suspend fun delete(packageName: String)

    @Insert
    abstract suspend fun logMuted(entity: MutedNotificationEntity)

    @Query("SELECT COUNT(*) FROM muted_notifications WHERE timestampMillis >= :sinceMillis")
    abstract suspend fun countMutedSince(sinceMillis: Long): Int
}
