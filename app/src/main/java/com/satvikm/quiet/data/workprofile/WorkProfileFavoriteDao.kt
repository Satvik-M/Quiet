package com.satvikm.quiet.data.workprofile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkProfileFavoriteDao {

    @Query("SELECT * FROM work_profile_favorites ORDER BY position ASC")
    abstract fun observeAll(): Flow<List<WorkProfileFavoriteEntity>>

    @Query("SELECT COUNT(*) FROM work_profile_favorites")
    abstract suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM work_profile_favorites WHERE appId = :appId)")
    abstract suspend fun isFavorite(appId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(favorite: WorkProfileFavoriteEntity)

    @Query("DELETE FROM work_profile_favorites WHERE appId = :appId")
    abstract suspend fun delete(appId: String)

    @Query("DELETE FROM work_profile_favorites")
    abstract suspend fun deleteAll()

    @Query("UPDATE work_profile_favorites SET position = :position WHERE appId = :appId")
    protected abstract suspend fun updatePosition(appId: String, position: Int)

    @Transaction
    open suspend fun reorder(appIdsInOrder: List<String>) {
        appIdsInOrder.forEachIndexed { index, appId -> updatePosition(appId, index) }
    }
}
