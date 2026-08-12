package com.satvikm.quiet.data.apps

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val label: String,
) {
    companion object {
        fun id(packageName: String, className: String, userSerial: Long) =
            "$packageName/$className/$userSerial"
    }
}
