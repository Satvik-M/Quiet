package com.satvikm.quiet.data.workprofile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkProfileAllowedAppDao {

    @Query("SELECT * FROM work_profile_allowed_apps")
    abstract fun observeAll(): Flow<List<WorkProfileAllowedAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM work_profile_allowed_apps WHERE appId = :appId)")
    abstract suspend fun isAllowed(appId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WorkProfileAllowedAppEntity)

    @Query("DELETE FROM work_profile_allowed_apps WHERE appId = :appId")
    abstract suspend fun delete(appId: String)

    @Query("DELETE FROM work_profile_allowed_apps")
    abstract suspend fun deleteAll()
}
