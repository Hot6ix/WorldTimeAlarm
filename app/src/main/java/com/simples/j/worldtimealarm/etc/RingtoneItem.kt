package com.simples.j.worldtimealarm.etc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simples.j.worldtimealarm.utils.DatabaseManager
import java.io.Serializable

/**
 * Created by j on 07/03/2018.
 *
 */
@Entity(tableName = DatabaseManager.TABLE_USER_RINGTONE)
data class RingtoneItem(
        @PrimaryKey var id: Int = -1,
        @ColumnInfo(name = DatabaseManager.COLUMN_TITLE) var title: String,
        @ColumnInfo(name = DatabaseManager.COLUMN_URI) var uri: String
): Serializable {
    override fun toString(): String {
        return "$title, $uri"
    }

    override fun equals(other: Any?): Boolean {
        val item = other as RingtoneItem
        return uri == item.uri
    }

    override fun hashCode(): Int {
        return uri.hashCode()
    }
}