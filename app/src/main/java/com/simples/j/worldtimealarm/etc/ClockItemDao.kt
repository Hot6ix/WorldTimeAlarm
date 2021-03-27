package com.simples.j.worldtimealarm.etc

import androidx.room.*
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Dao
interface ClockItemDao {
    @Query("SELECT * FROM ${DatabaseManager.TABLE_CLOCK_LIST} ORDER BY ${DatabaseManager.COLUMN_INDEX} ASC")
    suspend fun getAll(): List<ClockItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClockItem): Long

    @Delete
    suspend fun delete(item: ClockItem)

    @Update
    suspend fun update(item: ClockItem)

    // wapClockOrder(from: ClockItem, to: ClockItem) is no longer available, use update(item: ClockItem)
}