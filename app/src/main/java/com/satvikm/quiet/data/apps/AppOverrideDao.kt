package com.satvikm.quiet.data.apps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppOverrideDao {

    @Query("SELECT * FROM app_overrides")
    abstract fun observeAll(): Flow<List<AppOverrideEntity>>

    @Query("SELECT * FROM app_overrides WHERE appId = :appId")
    abstract suspend fun get(appId: String): AppOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(override: AppOverrideEntity)

    @Query("DELETE FROM app_overrides WHERE appId = :appId")
    abstract suspend fun delete(appId: String)
}
