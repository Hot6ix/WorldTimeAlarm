package com.simples.j.worldtimealarm.utils

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.etc.DstItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by j on 26/02/2018.
 *
 */
class DatabaseCursor(val context: Context) {

    private var dbManager = DatabaseManager(context)

    fun insertAlarm(item: AlarmItem): Long {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
            put(DatabaseManager.COLUMN_TIME_SET, item.timeSet)
            put(DatabaseManager.COLUMN_REPEAT, item.repeat.contentToString())
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
            put(DatabaseManager.COLUMN_PICKER_TIME, item.pickerTime)
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

    fun getSingleAlarm(alarmId: Int?): AlarmItem? {
        if(alarmId == null) return null

        val db = dbManager.readableDatabase

        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, null, "${DatabaseManager.COLUMN_ID} = ?", arrayOf(alarmId.toString()), null, null, null)

        var item: AlarmItem? = null

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
            val pickerTime = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_PICKER_TIME))

            val repeatValues = repeat.split(",").map { it.trim().toInt() }.toIntArray()
            val vibrationValues =
                    if(vibration == "null") null
                    else vibration.split(",").map { it.trim().toLong() }.toLongArray()

            item = AlarmItem(
                    id,
                    timeZone,
                    timeSet,
                    repeatValues,
                    ringtone,
                    vibrationValues,
                    snooze.toLong(),
                    label,
                    switch,
                    notiId,
                    colorTag,
                    index,
                    startDate,
                    endDate,
                    pickerTime
            )
        }

        cursor.close()
        db.close()

        return item
    }

    fun getSingleAlarmByNotificationId(nId: Int?): AlarmItem? {
        if(nId == null) return null

        val db = dbManager.readableDatabase

        val cursor = db.query(DatabaseManager.TABLE_ALARM_LIST, null, "${DatabaseManager.COLUMN_NOTI_ID} = ?", arrayOf(nId.toString()), null, null, null)

        var item: AlarmItem? = null

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
            val pickerTime = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_PICKER_TIME))

            val repeatValues = repeat.split(",").map { it.trim().toInt() }.toIntArray()
            val vibrationValues =
                    if(vibration == "null") null
                    else vibration.split(",").map { it.trim().toLong() }.toLongArray()

            item = AlarmItem(
                    id,
                    timeZone,
                    timeSet,
                    repeatValues,
                    ringtone,
                    vibrationValues,
                    snooze.toLong(),
                    label,
                    switch,
                    notiId,
                    colorTag,
                    index,
                    startDate,
                    endDate,
                    pickerTime
            )
        }

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
                val pickerTime = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_PICKER_TIME))

                val formattedRepeat =
                        if(repeat.isEmpty()) {
                            IntArray(7) { 0 }
                        }
                        else {
                            repeat.split(",").map { it.trim().toInt() }.toIntArray()
                        }

                val formattedVibration =
                        if(vibration.isEmpty() || vibration == "null" ) {
                            null
                        }
                        else {
                            vibration.split(",").map { it.trim().toLong() }.toLongArray()
                        }

                val item = AlarmItem(
                        id,
                        timeZone,
                        timeSet,
                        formattedRepeat,
                        ringtone,
                        formattedVibration,
                        snooze.toLong(),
                        label,
                        switch,
                        notiId,
                        colorTag,
                        index,
                        startDate,
                        endDate,
                        pickerTime
                )
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
                val pickerTime = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_PICKER_TIME))

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
                        endDate,
                        pickerTime
                )
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
            put(DatabaseManager.COLUMN_REPEAT, item.repeat.contentToString())
            put(DatabaseManager.COLUMN_RINGTONE, item.ringtone)
            put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(item.vibration))
            put(DatabaseManager.COLUMN_SNOOZE, item.snooze)
            put(DatabaseManager.COLUMN_LABEL, item.label)
            put(DatabaseManager.COLUMN_ON_OFF, item.on_off)
            put(DatabaseManager.COLUMN_NOTI_ID, item.notiId)
            put(DatabaseManager.COLUMN_COLOR_TAG, item.colorTag)
            put(DatabaseManager.COLUMN_START_DATE, item.startDate)
            put(DatabaseManager.COLUMN_END_DATE, item.endDate)
            put(DatabaseManager.COLUMN_PICKER_TIME, item.pickerTime)
        }

        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_NOTI_ID + "= ?", arrayOf(item.notiId.toString()))
        db.close()
    }

    fun updateAlarmIndex(item: AlarmItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_INDEX, item.index)
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
        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(from.id.toString()))

        // Change from item order
        contentValues.clear()
        contentValues.put(DatabaseManager.COLUMN_INDEX, fromOrder)
        db.update(DatabaseManager.TABLE_ALARM_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(to.id.toString()))

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
        db.update(DatabaseManager.TABLE_CLOCK_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(from.id.toString()))

        // Change from item order
        contentValues.clear()
        contentValues.put(DatabaseManager.COLUMN_INDEX, fromOrder)
        db.update(DatabaseManager.TABLE_CLOCK_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(to.id.toString()))

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
        val itemId = item.id
        if(itemId != null)
            db.delete(DatabaseManager.TABLE_CLOCK_LIST, DatabaseManager.COLUMN_ID + "= ?", arrayOf(itemId.toString()))
        else
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

    fun getUserRingtoneList(): ArrayList<RingtoneItem> {
        val db = dbManager.readableDatabase
        val ringtoneList = ArrayList<RingtoneItem>()

        val cursor = db.query(DatabaseManager.TABLE_USER_RINGTONE, null, null, null, null, null, DatabaseManager.COLUMN_TITLE + " ASC")
        if(cursor.count > 0) {
            while(cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TITLE))
                val uri = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_URI))

                val item = RingtoneItem(
                        title,
                        uri)

                val isAvailable = try {
                    val itemUri = Uri.parse(uri)
                    val docFile = DocumentFile.fromSingleUri(context, itemUri)
                    docFile?.exists() ?: false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

                if(isAvailable) ringtoneList.add(item)
                else removeUserRingtone(uri)
            }
        }

        cursor.close()
        db.close()

        return ringtoneList
    }

    fun findUserRingtone(userRingtoneUri: String): RingtoneItem? {
        val db = dbManager.readableDatabase
        var ringtoneItem: RingtoneItem? = null

        val cursor = db.query(DatabaseManager.TABLE_USER_RINGTONE, null, "${DatabaseManager.COLUMN_URI}=?", arrayOf(userRingtoneUri), null, null, null)
        if(cursor.count > 0) {
            cursor.moveToNext()
            val title = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TITLE))
            val uri = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_URI))

            ringtoneItem = RingtoneItem(title, uri)
        }
        cursor.close()

        return ringtoneItem
    }

    fun insertUserRingtone(ringtone: RingtoneItem): Boolean {
        val db = dbManager.writableDatabase

        val cursor = db.query(DatabaseManager.TABLE_USER_RINGTONE, null, "${DatabaseManager.COLUMN_URI}=?", arrayOf(ringtone.uri), null, null, null, null)
        if(cursor.count > 0) {
            cursor.close()
            db.close()

            return false
        }

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_TITLE, ringtone.title)
            put(DatabaseManager.COLUMN_URI, ringtone.uri)
        }

        db.insert(DatabaseManager.TABLE_USER_RINGTONE, null, contentValues)
        db.close()

        return true
    }

    fun removeUserRingtone(uri: String) {
        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_USER_RINGTONE, DatabaseManager.COLUMN_URI + "= ?", arrayOf(uri))
        db.close()
    }

    fun getSystemDst(): DstItem? {
        val db = dbManager.readableDatabase

        var systemDst: DstItem? = null

        val cursor = db.query(DatabaseManager.TABLE_DST_LIST, null, "${DatabaseManager.COLUMN_ALARM_ID}=?", arrayOf((-1).toString()), null, null, null)
        if(cursor.count > 0) {
            cursor.moveToNext()
            val id = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
            val timeSet = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_SET))
            val timeZone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
            val alarmId = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ALARM_ID))

            systemDst = DstItem(id, timeSet, timeZone, alarmId)
        }

        cursor.close()

        return systemDst
    }

    fun removeSystemDst() {
        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_DST_LIST, DatabaseManager.COLUMN_ALARM_ID + "= ?", arrayOf((-1).toString()))
        db.close()
    }

    fun getDstList(): ArrayList<DstItem> {
        val db = dbManager.readableDatabase
        val list = ArrayList<DstItem>()

        val cursor = db.query(DatabaseManager.TABLE_DST_LIST, null, null, null, null, null, null)
        if(cursor.count > 0) {
            while(cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                val timeSet = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_SET))
                val timeZone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
                val alarmId = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ALARM_ID))

                list.add(DstItem(id, timeSet, timeZone, alarmId))
            }
        }
        cursor.close()

        return list
    }

    fun findDstItemByAlarmId(alarmId: Int?): DstItem? {
        val db = dbManager.readableDatabase
        var dstItem: DstItem? = null

        val cursor = db.query(DatabaseManager.TABLE_DST_LIST, null, "${DatabaseManager.COLUMN_ALARM_ID}=?", arrayOf(alarmId.toString()), null, null, null)
        if(cursor.count > 0) {
            cursor.moveToNext()
            val id = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
            val millis = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_SET))
            val timeZone = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_TIME_ZONE))
            val aId = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ALARM_ID))

            dstItem = DstItem(id, millis, timeZone, aId)
        }
        cursor.close()

        return dstItem
    }

    fun insertDst(timeInMillis: Long, timeZone: String, alarmId: Int?): Long {
        val db = dbManager.writableDatabase

        if(alarmId == null) return -1

        val contentValues = ContentValues().apply {
            put(DatabaseManager.COLUMN_TIME_SET, timeInMillis)
            put(DatabaseManager.COLUMN_TIME_ZONE, timeZone)
            put(DatabaseManager.COLUMN_ALARM_ID, alarmId)
        }

        val id = db.insert(DatabaseManager.TABLE_DST_LIST, null, contentValues)
        db.close()

        return id
    }

    fun removeDst(dstItem: DstItem?) {
        if(dstItem == null) return

        val db = dbManager.writableDatabase
        db.delete(DatabaseManager.TABLE_DST_LIST, DatabaseManager.COLUMN_ID + "= ?", arrayOf(dstItem.id.toString()))
        db.close()
    }

    fun updateDst(item: DstItem) {
        val db = dbManager.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(DatabaseManager.COLUMN_TIME_SET, item.millis)
        contentValues.put(DatabaseManager.COLUMN_TIME_ZONE, item.timeZone)
        contentValues.put(DatabaseManager.COLUMN_ALARM_ID, item.alarmId)

        db.update(DatabaseManager.TABLE_DST_LIST, contentValues, DatabaseManager.COLUMN_ID + "= ?", arrayOf(item.id.toString()))
        db.close()
    }

    companion object {
        const val WRONG_ID = -1
    }

}