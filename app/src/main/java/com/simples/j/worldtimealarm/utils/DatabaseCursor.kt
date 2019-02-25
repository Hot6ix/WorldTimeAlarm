package com.simples.j.worldtimealarm.utils

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.ClockItem
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by j on 26/02/2018.
 *
 */
class DatabaseCursor(context: Context) {

    private var dbManager = DatabaseManager(context)

    fun insertAlarm(item: AlarmItem): Long {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
            put(DatabaseManager.COLUMN_TIME_SET, item.timeSet)
            put(DatabaseManager.COLUMN_REPEAT, Arrays.toString(item.repeat))
            put(DatabaseManager.COLUMN_RINGTONE, item.ringtone)
            put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(item.vibration))
            put(DatabaseManager.COLUMN_SNOOZE, item.snooze)
            put(DatabaseManager.COLUMN_LABEL, item.label)
            put(DatabaseManager.COLUMN_ON_OFF, item.on_off)
            put(DatabaseManager.COLUMN_NOTI_ID, item.notiId)
            put(DatabaseManager.COLUMN_COLOR_TAG, item.colorTag)
            put(DatabaseManager.COLUMN_INDEX, item.index)
            put(DatabaseManager.COLUMN_START_DATE, item.startDate)
            put(DatabaseManager.COLUMN_END_DATE, item.endDate)
        }

        val id = db.insert(DatabaseManager.TABLE_ALARM_LIST, null, contentValues)
        if(item.index == -1) {
            contentValues.clear()
            contentValues.put(DatabaseManager.COLUMN_INDEX, id)
            db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, "${DatabaseManager.COLUMN_ID} = ?", arrayOf(id.toString()))
        }
        db.close()

        return id
    }

    fun getSingleAlarm(alarmId: Int): AlarmItem {
        val db = dbManager.readableDatabase

        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, null, "${DatabaseManager.COLUMN_ID} = ?", arrayOf(alarmId.toString()), null, null, null)
        cursor.moveToFirst()
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
        val index = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_INDEX))
        val startDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_START_DATE))
        val endDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_END_DATE))

        val repeatValues = repeat.split(",").map { it.trim().toInt() }.toIntArray()
        val vibrationValues = if(vibration == "null") null else vibration.split(",").map { it.trim().toLong() }.toLongArray()
        val item = AlarmItem(id, timeZone, timeSet, repeatValues, ringtone, vibrationValues, snooze.toLong(), label, switch, notiId, colorTag, index, startDate, endDate)
        cursor.close()
        db.close()

        return item
    }

    fun getAlarmList(): ArrayList<AlarmItem> {
        val db = dbManager.readableDatabase
        val alarmList = ArrayList<AlarmItem>()

        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, null, null, null, null, null, DatabaseManager.COLUMN_INDEX + " ASC")
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
                val index = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_INDEX))
                val startDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_START_DATE))
                val endDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_END_DATE))

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
                        colorTag,
                        index,
                        startDate,
                        endDate)
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

        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, null, DatabaseManager.COLUMN_ON_OFF + "= ?", arrayOf("1"), null, null, null)
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
                val index = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_INDEX))
                val startDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_START_DATE))
                val endDate = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_END_DATE))

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
                        colorTag,
                        index,
                        startDate,
                        endDate)
                alarmList.add(item)
            }
        }

        cursor.close()
        db.close()

        return alarmList
    }

    fun removeAlarm(notiId: Int) {
        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_ALARM_LIST, DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(notiId.toString()))
        db.close()
    }

    fun updateAlarm(item: AlarmItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
            put(DatabaseManager.COLUMN_TIME_SET, item.timeSet)
            put(DatabaseManager.COLUMN_REPEAT, Arrays.toString(item.repeat))
            put(DatabaseManager.COLUMN_RINGTONE, item.ringtone)
            put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(item.vibration))
            put(DatabaseManager.COLUMN_SNOOZE, item.snooze)
            put(DatabaseManager.COLUMN_LABEL, item.label)
            put(DatabaseManager.COLUMN_ON_OFF, item.on_off)
            put(DatabaseManager.COLUMN_NOTI_ID, item.notiId)
            put(DatabaseManager.COLUMN_COLOR_TAG, item.colorTag)
            put(DatabaseManager.COLUMN_INDEX, item.index)
            put(DatabaseManager.COLUMN_START_DATE, item.startDate)
            put(DatabaseManager.COLUMN_END_DATE, item.endDate)
        }

        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(item.notiId.toString()))
        db.close()
    }

    fun swapAlarmOrder(from: AlarmItem, to: AlarmItem) {
        val db = dbManager.writableDatabase

        val fromOrder = from.index
        val toOrder = to.index

        // Change from item order
        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_INDEX, toOrder)
        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(from.id!!.toString()))

        // Change from item order
        contentValues.clear()
        contentValues.put(DatabaseManager.COLUMN_INDEX, fromOrder)
        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(to.id!!.toString()))

        db.close()
    }

    fun updateAlarmOnOffByNotiId(notiId: Int, switch: Boolean) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_ON_OFF, if(switch) 1 else 0)

        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_NOTI_ID + " = ?", arrayOf(notiId.toString()))
        db.close()
    }

    fun getAlarmId(notiId: Int): Int {
        val db = dbManager.readableDatabase

        var id = WRONG_ID
        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, arrayOf(DatabaseManager.COLUMN_ID), DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(notiId.toString()), null, null, null)
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
        val count = DatabaseUtils.queryNumEntries(db, DatabaseManager.TABLE_ALARM_LIST)
        db.close()
        return count
    }

    fun getDbVersion(): Int {
        val db = dbManager.readableDatabase
        return db.version
    }

    fun insertClock(item: ClockItem): Long {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_TIME_ZONE, item.timezone)
        contentValues.put(DatabaseManager.COLUMN_INDEX, item.index)

        val id = db.insert(DatabaseManager.TABLE_CLOCK_LIST, null, contentValues)
        if(item.index == -1) {
            contentValues.clear()
            contentValues.put(DatabaseManager.COLUMN_INDEX, id)
            db.update(DatabaseManager.TABLE_CLOCK_LIST, contentValues, "${DatabaseManager.COLUMN_ID} = ?", arrayOf(id.toString()))
        }
        db.close()

        return id
    }

    fun swapClockOrder(from: ClockItem, to: ClockItem) {
        val db = dbManager.writableDatabase

        val fromOrder = from.index
        val toOrder = to.index

        // Change from item order
        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_INDEX, toOrder)
        db.update(DatabaseManager.TABLE_CLOCK_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(from.id!!.toString()))

        // Change from item order
        contentValues.clear()
        contentValues.put(DatabaseManager.COLUMN_INDEX, fromOrder)
        db.update(DatabaseManager.TABLE_CLOCK_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(to.id!!.toString()))

        db.close()
    }

    fun getClockList(): ArrayList<ClockItem> {
        val db = dbManager.readableDatabase
        val clockList = ArrayList<ClockItem>()

        val cursor = db.query(DatabaseManager.TABLE_CLOCK_LIST, null, null, null, null, null, DatabaseManager.COLUMN_INDEX + " ASC")
        if(cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                val timezone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
                val index = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_INDEX))
                val item = ClockItem(
                        id,
                        timezone,
                        index)
                clockList.add(item)
            }
        }

        cursor.close()
        db.close()

        return clockList
    }

    fun removeClock(item: ClockItem) {
        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_CLOCK_LIST, DatabaseManager.COLUMN_TIME_ZONE + "= ?", arrayOf(item.timezone))
        db.close()
    }

    fun updateClockItem(item: ClockItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_TIME_ZONE, item.timezone)

        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(item.id.toString()))
        db.close()
    }

    companion object {
        const val WRONG_ID = -1
    }

}