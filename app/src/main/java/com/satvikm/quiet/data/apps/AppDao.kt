package com.satvikm.quiet.data.apps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {

    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE ASC")
    abstract fun observeAll(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(apps: List<AppEntity>)

    @Query("DELETE FROM apps WHERE userSerial = :userSerial")
    protected abstract suspend fun deleteForUser(userSerial: Long)

    @Query("DELETE FROM apps WHERE packageName = :packageName AND userSerial = :userSerial")
    protected abstract suspend fun deleteForPackage(packageName: String, userSerial: Long)

    /** Replaces the full app set for one profile, e.g. after a startup scan. */
    @Transaction
    open suspend fun replaceForUser(userSerial: Long, apps: List<AppEntity>) {
        deleteForUser(userSerial)
        insertAll(apps)
    }

    /** Replaces one package's activities for one profile, e.g. after an install/update/uninstall. */
    @Transaction
    open suspend fun replacePackageForUser(packageName: String, userSerial: Long, apps: List<AppEntity>) {
        deleteForPackage(packageName, userSerial)
        insertAll(apps)
    }
}
