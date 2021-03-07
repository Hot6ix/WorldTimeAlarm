package com.simples.j.worldtimealarm.etc

import androidx.room.*
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Dao
interface DstItemDao {
    @Query("SELECT * FROM ${DatabaseManager.TABLE_DST_LIST}")
    fun getAll(): List<DstItem>

    @Query("SELECT * FROM ${DatabaseManager.TABLE_DST_LIST} WHERE ${DatabaseManager.COLUMN_ALARM_ID} = -1")
    fun getSystemDst(): DstItem?

    @Delete
    fun delete(item: DstItem)

    @Query("DELETE FROM ${DatabaseManager.TABLE_DST_LIST} WHERE ${DatabaseManager.COLUMN_ALARM_ID} = -1")
    fun deleteSystemDst()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: DstItem): Long

    @Update
    fun update(item: DstItem)
}