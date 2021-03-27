package com.simples.j.worldtimealarm.etc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Entity(tableName = DatabaseManager.TABLE_DST_LIST)
data class DstItem(
        @PrimaryKey val id: Long,
        @ColumnInfo(name = DatabaseManager.COLUMN_TIME_SET) var millis: Long,
        @ColumnInfo(name = DatabaseManager.COLUMN_TIME_ZONE) var timeZone: String,
        @ColumnInfo(name = DatabaseManager.COLUMN_ALARM_ID) var alarmId: Int?
)