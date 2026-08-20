package com.satvikm.quiet.data.block

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class GraceDao {

    @Query("SELECT * FROM friction_grace WHERE graceUntilMillis > :nowMillis")
    abstract suspend fun getAllActive(nowMillis: Long): List<GraceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: GraceEntity)

    @Query("DELETE FROM friction_grace WHERE graceUntilMillis <= :nowMillis")
    abstract suspend fun deleteExpired(nowMillis: Long)
}
