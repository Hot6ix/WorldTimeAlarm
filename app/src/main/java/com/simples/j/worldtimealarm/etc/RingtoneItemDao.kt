package com.simples.j.worldtimealarm.etc

import androidx.room.*
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Dao
interface RingtoneItemDao {
    @Query("SELECT * FROM ${DatabaseManager.TABLE_USER_RINGTONE}")
    suspend fun getAll(): List<RingtoneItem>

    @Query("SELECT * FROM ${DatabaseManager.TABLE_USER_RINGTONE} WHERE ${DatabaseManager.COLUMN_URI} = :uri")
    suspend fun getRingtoneFromUri(uri: String): RingtoneItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: RingtoneItem)

    @Delete
    fun delete(item: RingtoneItem)

    @Query("DELETE FROM ${DatabaseManager.TABLE_USER_RINGTONE} WHERE ${DatabaseManager.COLUMN_URI} = :uri")
    fun delete(uri: String)
}