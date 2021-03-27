package com.simples.j.worldtimealarm.etc

import androidx.room.*
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Dao
interface AlarmItemDao {
    @Query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST} ORDER BY ${DatabaseManager.COLUMN_INDEX} ASC")
    suspend fun getAll(): List<AlarmItem>

    @Query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST} WHERE ${DatabaseManager.COLUMN_ON_OFF} = 1")
    suspend fun getActivated(): List<AlarmItem>

    @Query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST} WHERE ${DatabaseManager.COLUMN_ID} = :alarmId")
    suspend fun getAlarmItemFromId(alarmId: Int?): AlarmItem?

    @Query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST} WHERE ${DatabaseManager.COLUMN_NOTI_ID} = :notificationId")
    suspend fun getAlarmItemFromNotificationId(notificationId: Int?): AlarmItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AlarmItem): Long

    @Delete
    suspend fun delete(item: AlarmItem)

    @Query("DELETE FROM ${DatabaseManager.TABLE_ALARM_LIST} WHERE ${DatabaseManager.COLUMN_NOTI_ID} = :notificationId")
    suspend fun delete(notificationId: Int)

    @Update
    suspend fun update(item: AlarmItem)

    @Update
    fun updateItem(item: AlarmItem)

    /*
        updateIndex(item: AlarmItem) and swapAlarmOrder(from: AlarmItem, to: AlarmItem)
        are no longer available, use update(item: AlarmItem) above
    */
}