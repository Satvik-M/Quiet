package com.satvikm.quiet.data.block

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BlockedAppDao {

    @Query("SELECT * FROM blocked_apps")
    abstract fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    abstract suspend fun get(packageName: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    abstract suspend fun delete(packageName: String)

    @Query("DELETE FROM blocked_apps")
    abstract suspend fun deleteAll()

    @Insert
    abstract suspend fun logOpen(entity: AppOpenEntity)

    @Query("SELECT COUNT(*) FROM app_opens WHERE packageName = :packageName AND timestampMillis >= :sinceMillis")
    abstract suspend fun countOpensSince(packageName: String, sinceMillis: Long): Int
}
