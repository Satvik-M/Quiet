package com.satvikm.quiet.data.focus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FocusAutoMuteDao {

    @Query("SELECT * FROM focus_auto_muted_apps")
    abstract fun observeAll(): Flow<List<FocusAutoMutedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<FocusAutoMutedAppEntity>)

    @Query("DELETE FROM focus_auto_muted_apps")
    abstract suspend fun clearAll()

    @Transaction
    open suspend fun replaceAll(packageNames: Set<String>) {
        clearAll()
        insertAll(packageNames.map { FocusAutoMutedAppEntity(it) })
    }
}
