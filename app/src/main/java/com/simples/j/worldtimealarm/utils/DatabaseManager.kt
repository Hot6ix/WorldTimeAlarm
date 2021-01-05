package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by j on 26/02/2018.
 *
 */
class DatabaseManager(val context: Context): SQLiteOpenHelper(context, DB_NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)

        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALARM_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TIME_ZONE TEXT," +
                "$COLUMN_TIME_SET TEXT," +
                "$COLUMN_REPEAT TEXT," +
                "$COLUMN_RINGTONE TEXT," +
                "$COLUMN_VIBRATION TEXT," +
                "$COLUMN_SNOOZE INTEGER," +
                "$COLUMN_LABEL TEXT," +
                "$COLUMN_ON_OFF INTEGER," +
                "$COLUMN_NOTI_ID INTEGER," +
                "$COLUMN_COLOR_TAG INTEGER," +
                "$COLUMN_INDEX INTEGER," +
                "$COLUMN_START_DATE INTEGER," +
                "$COLUMN_END_DATE INTEGER," +
                "$COLUMN_PICKER_TIME TEXT);")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CLOCK_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TIME_ZONE TEXT, " +
                "$COLUMN_INDEX INTEGER);")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_USER_RINGTONE ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT, " +
                "$COLUMN_URI TEXT);")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_DST_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TIME_SET INTEGER, " +
                "$COLUMN_TIME_ZONE TEXT, " +
                "$COLUMN_ALARM_ID INTEGER);")
    }

    // TODO: Check SQLiteException
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        if(old < 2) {
            db.execSQL("ALTER TABLE $TABLE_ALARM_LIST ADD COLUMN $COLUMN_COLOR_TAG INTEGER DEFAULT 0")
        }
        if(old < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CLOCK_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_TIME_ZONE TEXT," +
                    "$COLUMN_INDEX INTEGER);")
            db.execSQL("ALTER TABLE $TABLE_ALARM_LIST ADD COLUMN $COLUMN_INDEX INTEGER")
            db.execSQL("UPDATE $TABLE_ALARM_LIST SET $COLUMN_INDEX = $COLUMN_ID")
        }
        if(old < 4) {
            db.execSQL("ALTER TABLE $TABLE_ALARM_LIST ADD COLUMN $COLUMN_START_DATE INTEGER")
            db.execSQL("ALTER TABLE $TABLE_ALARM_LIST ADD COLUMN $COLUMN_END_DATE INTEGER")
        }
        if(old < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_USER_RINGTONE ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_TITLE TEXT, " +
                    "$COLUMN_URI TEXT);")
        }
        if(old < 7) {
            db.execSQL("ALTER TABLE $TABLE_ALARM_LIST ADD COLUMN $COLUMN_PICKER_TIME TEXT")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_DST_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_TIME_SET INTEGER, " +
                    "$COLUMN_TIME_ZONE TEXT, " +
                    "$COLUMN_ALARM_ID INTEGER);")
        }
    }

    companion object {
        const val VERSION = 7
        const val DB_NAME = "alarm.db"

        const val TABLE_ALARM_LIST = "AlarmList"
        const val TABLE_CLOCK_LIST = "ClockList"
        const val TABLE_USER_RINGTONE = "Ringtone"
        const val TABLE_DST_LIST = "DaylightSavingTime"

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
        const val COLUMN_COLOR_TAG = "colorTag"
        const val COLUMN_INDEX = "dorder"
        const val COLUMN_START_DATE = "start_date"
        const val COLUMN_END_DATE = "end_date"
        const val COLUMN_TITLE = "title"
        const val COLUMN_URI = "uri"
        const val COLUMN_PICKER_TIME = "picker_time"
        const val COLUMN_ALARM_ID = "alarm_id"
    }

}