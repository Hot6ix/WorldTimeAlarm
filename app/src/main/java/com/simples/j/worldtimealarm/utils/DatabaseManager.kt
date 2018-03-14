package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by j on 26/02/2018.
 *
 */
class DatabaseManager(private val context: Context): SQLiteOpenHelper(context, DB_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TIME_ZONE TEXT, " +
                "$COLUMN_TIME_SET TEXT, " +
                "$COLUMN_REPEAT TEXT," +
                "$COLUMN_RINGTONE TEXT," +
                "$COLUMN_VIBRATION TEXT," +
                "$COLUMN_SNOOZE INTEGER," +
                "$COLUMN_LABEL TEXT," +
                "$COLUMN_ON_OFF INTEGER," +
                "$COLUMN_NOTI_ID INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val VERSION = 1
        const val DB_NAME = "alarm.db"

        const val TABLE_NAME = "AlarmList"
        const val COLUMN_ID = "id"
        const val COLUMN_TIME_ZONE = "timezone"
        const val COLUMN_TIME_SET = "time_set"
        const val COLUMN_REPEAT = "repeat"
        const val COLUMN_RINGTONE = "ringtone"
        const val COLUMN_VIBRATION = "vibration"
        const val COLUMN_SNOOZE = "snooze"
        const val COLUMN_LABEL = "label"
        const val COLUMN_ON_OFF = "on_off"
        const val COLUMN_NOTI_ID = "notiId"
    }

}