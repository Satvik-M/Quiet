package com.satvikm.quiet.data.favorites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY position ASC")
    abstract fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    abstract suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE appId = :appId)")
    abstract suspend fun isFavorite(appId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE appId = :appId")
    abstract suspend fun delete(appId: String)
}
