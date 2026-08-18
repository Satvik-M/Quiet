package com.satvikm.quiet.data.focus

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FocusScheduleDao {

    @Query("SELECT * FROM focus_schedules")
    abstract fun observeAll(): Flow<List<FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules")
    abstract suspend fun getAll(): List<FocusScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: FocusScheduleEntity)

    @Delete
    abstract suspend fun delete(entity: FocusScheduleEntity)
}
