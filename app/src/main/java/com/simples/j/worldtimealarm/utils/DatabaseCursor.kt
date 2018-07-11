package com.simples.j.worldtimealarm.utils

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import com.simples.j.worldtimealarm.etc.AlarmItem
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by j on 26/02/2018.
 *
 */
class DatabaseCursor(context: Context) {

    private var dbManager = DatabaseManager(context)

    fun insertAlarm(item: AlarmItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        if(item.id != null) contentValues.put(DatabaseManager.COLUMN_ID, item.id)
        contentValues.put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
        contentValues.put(DatabaseManager.COLUMN_TIME_SET, item.timeSet)
        contentValues.put(DatabaseManager.COLUMN_REPEAT, Arrays.toString(item.repeat))
        contentValues.put(DatabaseManager.COLUMN_RINGTONE, item.ringtone)
        contentValues.put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(item.vibration))
        contentValues.put(DatabaseManager.COLUMN_SNOOZE, item.snooze)
        contentValues.put(DatabaseManager.COLUMN_LABEL, item.label)
        contentValues.put(DatabaseManager.COLUMN_ON_OFF, item.on_off)
        contentValues.put(DatabaseManager.COLUMN_NOTI_ID, item.notiId)
        contentValues.put(DatabaseManager.COLUMN_COLOR_TAG, item.colorTag)

        db.insert(DatabaseManager.TABLE_NAME, null, contentValues)
        db.close()
    }

    fun getAlarmList(): ArrayList<AlarmItem> {
        val db = dbManager.readableDatabase
        val alarmList = ArrayList<AlarmItem>()

        val cursor = db.query(DatabaseManager.TABLE_NAME, null, null, null, null, null, DatabaseManager.COLUMN_ID + " ASC")
        if(cursor.count > 0) {
            while(cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                val timeZone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
                val timeSet = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_SET))
                val repeat = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_REPEAT)).replace("[", "").replace("]", "")
                val ringtone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_RINGTONE))
                val vibration = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_VIBRATION)).replace("[", "").replace("]", "")
                val snooze = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_SNOOZE))
                val label = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_LABEL))
                val switch = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ON_OFF))
                val notiId = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_NOTI_ID))
                val colorTag = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_COLOR_TAG))

                val item = AlarmItem(
                        id,
                        timeZone,
                        timeSet,
                        repeat.split(",").map { it.trim().toInt() }.toIntArray(),
                        ringtone,
                        if(vibration == "null") null else vibration.split(",").map { it.trim().toLong() }.toLongArray(),
                        snooze.toLong(),
                        label,
                        switch,
                        notiId,
                        colorTag)
                alarmList.add(item)
            }
        }

        cursor.close()
        db.close()

        return alarmList
    }

    fun getActivatedAlarms(): ArrayList<AlarmItem> {
        val db = dbManager.readableDatabase
        val alarmList = ArrayList<AlarmItem>()

        val cursor = db.query(DatabaseManager.TABLE_NAME, null, DatabaseManager.COLUMN_ON_OFF + "= ?", arrayOf("1"), null, null, DatabaseManager.COLUMN_ID + " ASC")
        if(cursor.count > 0) {
            while(cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                val timeZone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
                val timeSet = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_SET))
                val repeat = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_REPEAT)).replace("[", "").replace("]", "")
                val ringtone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_RINGTONE))
                val vibration = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_VIBRATION)).replace("[", "").replace("]", "")
                val snooze = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_SNOOZE))
                val label = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_LABEL))
                val switch = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ON_OFF))
                val notiId = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_NOTI_ID))
                val colorTag = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_COLOR_TAG))

                val item = AlarmItem(
                        id,
                        timeZone,
                        timeSet,
                        repeat.split(",").map { it.trim().toInt() }.toIntArray(),
                        ringtone,
                        if(vibration == "null") null else vibration.split(",").map { it.trim().toLong() }.toLongArray(),
                        snooze.toLong(),
                        label,
                        switch,
                        notiId,
                        colorTag)
                alarmList.add(item)
            }
        }

        cursor.close()
        db.close()

        return alarmList
    }

    fun removeAlarm(notiId: Int) {
        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_NAME, DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(notiId.toString()))
        db.close()
    }

    fun updateAlarm(item: AlarmItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
        contentValues.put(DatabaseManager.COLUMN_TIME_SET, item.timeSet)
        contentValues.put(DatabaseManager.COLUMN_REPEAT, Arrays.toString(item.repeat))
        contentValues.put(DatabaseManager.COLUMN_RINGTONE, item.ringtone)
        contentValues.put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(item.vibration))
        contentValues.put(DatabaseManager.COLUMN_SNOOZE, item.snooze)
        contentValues.put(DatabaseManager.COLUMN_LABEL, item.label)
        contentValues.put(DatabaseManager.COLUMN_ON_OFF, item.on_off)
        contentValues.put(DatabaseManager.COLUMN_NOTI_ID, item.notiId)
        contentValues.put(DatabaseManager.COLUMN_COLOR_TAG, item.colorTag)

        db.update(DatabaseManager.TABLE_NAME, contentValues, DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(item.notiId.toString()))
        db.close()
    }

    fun updateAlarmOnOffByNotiId(notiId: Int, switch: Boolean) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_ON_OFF, if(switch) 1 else 0)

        db.update(DatabaseManager.TABLE_NAME, contentValues, DatabaseManager.COLUMN_NOTI_ID + " = ?", arrayOf(notiId.toString()))
        db.close()
    }

    fun getAlarmId(notiId: Int): Int {
        val db = dbManager.readableDatabase

        var id = WRONG_ID
        val cursor = db.query(DatabaseManager.TABLE_NAME, arrayOf(DatabaseManager.COLUMN_ID), DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(notiId.toString()), null, null, null)
        if(cursor.count > 0) {
            while(cursor.moveToNext()) {
                id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
            }
        }
        cursor.close()
        db.close()

        return id
    }

    fun getAlarmListSize(): Long {
        val db = dbManager.readableDatabase
        val count = DatabaseUtils.queryNumEntries(db, DatabaseManager.TABLE_NAME)
        db.close()
        return count
    }

    fun getDbVersion(): Int {
        val db = dbManager.readableDatabase
        return db.version
    }

    companion object {
        const val WRONG_ID = -1
    }

}