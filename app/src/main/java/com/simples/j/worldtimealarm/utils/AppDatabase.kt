package com.simples.j.worldtimealarm.utils

import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simples.j.worldtimealarm.etc.*

@Database(entities = [AlarmItem::class, ClockItem::class, DstItem::class, RingtoneItem::class], version = 8)
@TypeConverters(com.simples.j.worldtimealarm.utils.TypeConverters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun alarmItemDao(): AlarmItemDao
    abstract fun clockItemDao(): ClockItemDao
    abstract fun dstItemDao(): DstItemDao
    abstract fun ringtoneItemDao(): RingtoneItemDao

    companion object {
        val MIGRATION_4_8 = object : Migration(4, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(C.TAG, "Migrate from 4 to 8")
                // due to migration issue, create new table and copy data to new table
                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_TIME_SET} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_REPEAT} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_RINGTONE} TEXT," +
                        "${DatabaseManager.COLUMN_VIBRATION} TEXT," +
                        "${DatabaseManager.COLUMN_SNOOZE} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_LABEL} TEXT," +
                        "${DatabaseManager.COLUMN_ON_OFF} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_NOTI_ID} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_COLOR_TAG} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER," +
                        "${DatabaseManager.COLUMN_START_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_END_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_PICKER_TIME} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_DAY_OF_WEEK_ORDINAL} TEXT" +
                        ");")

                database.execSQL("INSERT INTO TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_PICKER_TIME}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_TIME_SET} " +
                        "FROM ${DatabaseManager.TABLE_ALARM_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_ALARM_LIST}")
                database.execSQL("ALTER TABLE TMP_ALARM_LIST RENAME TO ${DatabaseManager.TABLE_ALARM_LIST}")

                // get id and recurrences of all items
                val cursor = database.query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST}")
                if(cursor.count > 0) {
                    val list = ArrayList<Pair<Int, IntArray>>()

                    while(cursor.moveToNext()) {
                        val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                        val repeat = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_REPEAT)).replace("[", "").replace("]", "")

                        val formattedRepeat =
                                if(repeat.isEmpty()) {
                                    IntArray(7) { 0 }
                                }
                                else {
                                    repeat.split(",").map { it.trim().toInt() }.toIntArray()
                                }

                        list.add(Pair(id, formattedRepeat))
                    }

                    // convert recurrence value
                    val updated = list.map {
                        val updatedRecurrences = it.second.map { dayOfWeek ->
                            if(dayOfWeek != 0) {
                                var converted = dayOfWeek - 1
                                if(converted == 0) converted = 7

                                converted
                            }
                            else {
                                dayOfWeek
                            }
                        }.toTypedArray()

                        Pair(it.first, updatedRecurrences)
                    }

                    // update items
                    updated.forEach {
                        database.execSQL("UPDATE ${DatabaseManager.TABLE_ALARM_LIST} SET ${DatabaseManager.COLUMN_REPEAT} = '"+ it.second.contentToString() + "' WHERE ${DatabaseManager.COLUMN_ID} = ${it.first}")
                    }
                }

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT PRIMARY KEY NOT NULL, " +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER" +
                        ");")

                database.execSQL("INSERT INTO TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX} " +
                        "FROM ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_CLOCK_LIST}")
                database.execSQL("ALTER TABLE TMP_CLOCK_LIST RENAME TO ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_USER_RINGTONE} (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TITLE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_URI} TEXT NOT NULL" +
                        ");")

                database.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_DST_LIST} (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_SET} INTEGER NOT NULL, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_ALARM_ID} INTEGER" +
                        ");")
            }
        }
        val MIGRATION_5_8 = object : Migration(5, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(C.TAG, "Migrate from 5 to 8")
                // due to migration issue, create new table and copy data to new table
                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_TIME_SET} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_REPEAT} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_RINGTONE} TEXT," +
                        "${DatabaseManager.COLUMN_VIBRATION} TEXT," +
                        "${DatabaseManager.COLUMN_SNOOZE} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_LABEL} TEXT," +
                        "${DatabaseManager.COLUMN_ON_OFF} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_NOTI_ID} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_COLOR_TAG} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER," +
                        "${DatabaseManager.COLUMN_START_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_END_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_PICKER_TIME} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_DAY_OF_WEEK_ORDINAL} TEXT" +
                        ");")

                database.execSQL("INSERT INTO TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_PICKER_TIME}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_TIME_SET} " +
                        "FROM ${DatabaseManager.TABLE_ALARM_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_ALARM_LIST}")
                database.execSQL("ALTER TABLE TMP_ALARM_LIST RENAME TO ${DatabaseManager.TABLE_ALARM_LIST}")

                // get id and recurrences of all items
                val cursor = database.query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST}")
                if(cursor.count > 0) {
                    val list = ArrayList<Pair<Int, IntArray>>()

                    while(cursor.moveToNext()) {
                        val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                        val repeat = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_REPEAT)).replace("[", "").replace("]", "")

                        val formattedRepeat =
                                if(repeat.isEmpty()) {
                                    IntArray(7) { 0 }
                                }
                                else {
                                    repeat.split(",").map { it.trim().toInt() }.toIntArray()
                                }

                        list.add(Pair(id, formattedRepeat))
                    }

                    // convert recurrence value
                    val updated = list.map {
                        val updatedRecurrences = it.second.map { dayOfWeek ->
                            if(dayOfWeek != 0) {
                                var converted = dayOfWeek - 1
                                if(converted == 0) converted = 7

                                converted
                            }
                            else {
                                dayOfWeek
                            }
                        }.toTypedArray()

                        Pair(it.first, updatedRecurrences)
                    }

                    // update items
                    updated.forEach {
                        database.execSQL("UPDATE ${DatabaseManager.TABLE_ALARM_LIST} SET ${DatabaseManager.COLUMN_REPEAT} = '"+ it.second.contentToString() + "' WHERE ${DatabaseManager.COLUMN_ID} = ${it.first}")
                    }
                }

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT PRIMARY KEY NOT NULL, " +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER" +
                        ");")

                database.execSQL("INSERT INTO TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX} " +
                        "FROM ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_CLOCK_LIST}")
                database.execSQL("ALTER TABLE TMP_CLOCK_LIST RENAME TO ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_RINGTONE (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TITLE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_URI} TEXT NOT NULL" +
                        ");")

                database.execSQL("INSERT INTO TMP_RINGTONE(" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TITLE}, " +
                        "${DatabaseManager.COLUMN_URI}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TITLE}, " +
                        "${DatabaseManager.COLUMN_URI} " +
                        "FROM ${DatabaseManager.TABLE_USER_RINGTONE}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_USER_RINGTONE}")
                database.execSQL("ALTER TABLE TMP_RINGTONE RENAME TO ${DatabaseManager.TABLE_USER_RINGTONE}")

                database.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_DST_LIST} (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_SET} INTEGER NOT NULL, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_ALARM_ID} INTEGER" +
                        ");")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(C.TAG, "Migrate from 7 to 8")
                // due to migration issue, create new table and copy data to new table
                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_TIME_SET} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_REPEAT} TEXT NOT NULL," +
                        "${DatabaseManager.COLUMN_RINGTONE} TEXT," +
                        "${DatabaseManager.COLUMN_VIBRATION} TEXT," +
                        "${DatabaseManager.COLUMN_SNOOZE} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_LABEL} TEXT," +
                        "${DatabaseManager.COLUMN_ON_OFF} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_NOTI_ID} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_COLOR_TAG} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER," +
                        "${DatabaseManager.COLUMN_START_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_END_DATE} INTEGER," +
                        "${DatabaseManager.COLUMN_PICKER_TIME} INTEGER NOT NULL," +
                        "${DatabaseManager.COLUMN_DAY_OF_WEEK_ORDINAL} TEXT" +
                        ");")

                database.execSQL("INSERT INTO TMP_ALARM_LIST (" +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_PICKER_TIME}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}," +
                        "${DatabaseManager.COLUMN_TIME_ZONE}," +
                        "${DatabaseManager.COLUMN_TIME_SET}," +
                        "${DatabaseManager.COLUMN_REPEAT}," +
                        "${DatabaseManager.COLUMN_RINGTONE}," +
                        "${DatabaseManager.COLUMN_VIBRATION}," +
                        "${DatabaseManager.COLUMN_SNOOZE}," +
                        "${DatabaseManager.COLUMN_LABEL}," +
                        "${DatabaseManager.COLUMN_ON_OFF}," +
                        "${DatabaseManager.COLUMN_NOTI_ID}," +
                        "${DatabaseManager.COLUMN_COLOR_TAG}," +
                        "${DatabaseManager.COLUMN_INDEX}," +
                        "${DatabaseManager.COLUMN_START_DATE}," +
                        "${DatabaseManager.COLUMN_END_DATE}," +
                        "${DatabaseManager.COLUMN_PICKER_TIME} " +
                        "FROM ${DatabaseManager.TABLE_ALARM_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_ALARM_LIST}")
                database.execSQL("ALTER TABLE TMP_ALARM_LIST RENAME TO ${DatabaseManager.TABLE_ALARM_LIST}")

                // get id and recurrences of all items
                val cursor = database.query("SELECT * FROM ${DatabaseManager.TABLE_ALARM_LIST}")
                if(cursor.count > 0) {
                    val list = ArrayList<Pair<Int, IntArray>>()

                    while(cursor.moveToNext()) {
                        val id = cursor.getInt(cursor.getColumnIndex(DatabaseManager.COLUMN_ID))
                        val repeat = cursor.getString(cursor.getColumnIndex(DatabaseManager.COLUMN_REPEAT)).replace("[", "").replace("]", "")

                        val formattedRepeat =
                                if(repeat.isEmpty()) {
                                    IntArray(7) { 0 }
                                }
                                else {
                                    repeat.split(",").map { it.trim().toInt() }.toIntArray()
                                }

                        list.add(Pair(id, formattedRepeat))
                    }

                    // convert recurrence value
                    val updated = list.map {
                        val updatedRecurrences = it.second.map { dayOfWeek ->
                            if(dayOfWeek != 0) {
                                var converted = dayOfWeek - 1
                                if(converted == 0) converted = 7

                                converted
                            }
                            else {
                                dayOfWeek
                            }
                        }.toTypedArray()

                        Pair(it.first, updatedRecurrences)
                    }

                    // update items
                    updated.forEach {
                        database.execSQL("UPDATE ${DatabaseManager.TABLE_ALARM_LIST} SET ${DatabaseManager.COLUMN_REPEAT} = '"+ it.second.contentToString() + "' WHERE ${DatabaseManager.COLUMN_ID} = ${it.first}")
                    }
                }

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT PRIMARY KEY NOT NULL, " +
                        "${DatabaseManager.COLUMN_INDEX} INTEGER" +
                        ");")

                database.execSQL("INSERT INTO TMP_CLOCK_LIST (" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_INDEX} " +
                        "FROM ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_CLOCK_LIST}")
                database.execSQL("ALTER TABLE TMP_CLOCK_LIST RENAME TO ${DatabaseManager.TABLE_CLOCK_LIST}")

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_RINGTONE (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TITLE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_URI} TEXT NOT NULL" +
                        ");")

                database.execSQL("INSERT INTO TMP_RINGTONE(" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TITLE}, " +
                        "${DatabaseManager.COLUMN_URI}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TITLE}, " +
                        "${DatabaseManager.COLUMN_URI} " +
                        "FROM ${DatabaseManager.TABLE_USER_RINGTONE}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_USER_RINGTONE}")
                database.execSQL("ALTER TABLE TMP_RINGTONE RENAME TO ${DatabaseManager.TABLE_USER_RINGTONE}")

                database.execSQL("CREATE TABLE IF NOT EXISTS TMP_DAYLIGHT_SAVING_TIME (" +
                        "${DatabaseManager.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "${DatabaseManager.COLUMN_TIME_SET} INTEGER NOT NULL, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE} TEXT NOT NULL, " +
                        "${DatabaseManager.COLUMN_ALARM_ID} INTEGER" +
                        ");")

                database.execSQL("INSERT INTO TMP_DAYLIGHT_SAVING_TIME(" +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_SET}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_ALARM_ID}) " +
                        "SELECT " +
                        "${DatabaseManager.COLUMN_ID}, " +
                        "${DatabaseManager.COLUMN_TIME_SET}, " +
                        "${DatabaseManager.COLUMN_TIME_ZONE}, " +
                        "${DatabaseManager.COLUMN_ALARM_ID} " +
                        "FROM ${DatabaseManager.TABLE_DST_LIST}")

                database.execSQL("DROP TABLE ${DatabaseManager.TABLE_DST_LIST}")
                database.execSQL("ALTER TABLE TMP_DAYLIGHT_SAVING_TIME RENAME TO ${DatabaseManager.TABLE_DST_LIST}")
            }
        }
    }
}