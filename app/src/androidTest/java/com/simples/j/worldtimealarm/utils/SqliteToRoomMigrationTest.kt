package com.simples.j.worldtimealarm.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simples.j.worldtimealarm.TestUtils
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_ALARM_ID
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_COLOR_TAG
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_END_DATE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_ID
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_INDEX
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_LABEL
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_NOTI_ID
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_ON_OFF
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_PICKER_TIME
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_REPEAT
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_RINGTONE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_SNOOZE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_START_DATE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_TIME_SET
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_TIME_ZONE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_TITLE
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_URI
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.COLUMN_VIBRATION
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.TABLE_ALARM_LIST
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.TABLE_CLOCK_LIST
import com.simples.j.worldtimealarm.utils.DatabaseManager.Companion.TABLE_USER_RINGTONE
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class SqliteToRoomMigrationTest {

    private val TEST_DB = "migration_test_db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun init() {

    }

    @After
    fun terminate() {
    }

    /*
        To test migration from SQLite to Room,
        must delete app before each test start
    */

    @Test
    fun migrateSqlite_Room_4_8() {
        val db = TestDatabase(InstrumentationRegistry.getInstrumentation().targetContext, 4)
        db.insertSampleData()

        helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_4_8)
    }

    @Test
    fun migrateSqlite_Room_5_8() {
        val db = TestDatabase(InstrumentationRegistry.getInstrumentation().targetContext, 5)
        db.insertSampleData()

        helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_5_8)
    }

    @Test
    fun migrateSqlite_Room_7_8() {
        val db = TestDatabase(InstrumentationRegistry.getInstrumentation().targetContext, 7)
        db.insertSampleData()

        helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8)
    }

    // create test database
    inner class TestDatabase(val context: Context, private val version: Int): SQLiteOpenHelper(context, TEST_DB, null, version) {
        override fun onCreate(db: SQLiteDatabase?) {
            when(version) {
                4 -> {
                    println("Create database with version 4")
                    db?.execSQL("CREATE TABLE $TABLE_ALARM_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
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
                            "$COLUMN_END_DATE INTEGER);")

                    db?.execSQL("CREATE TABLE $TABLE_CLOCK_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "$COLUMN_TIME_ZONE TEXT, " +
                            "$COLUMN_INDEX INTEGER);")
                }
                5 -> {
                    println("Create database with version 5")
                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALARM_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
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
                            "$COLUMN_END_DATE INTEGER);")

                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CLOCK_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "$COLUMN_TIME_ZONE TEXT, " +
                            "$COLUMN_INDEX INTEGER);")

                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_USER_RINGTONE ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "$COLUMN_TITLE TEXT, " +
                            "$COLUMN_URI TEXT);")
                }
                7 -> {
                    println("Create database with version 7")
                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALARM_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
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

                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CLOCK_LIST ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "$COLUMN_TIME_ZONE TEXT, " +
                            "$COLUMN_INDEX INTEGER);")

                    db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_USER_RINGTONE ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "$COLUMN_TITLE TEXT, " +
                            "$COLUMN_URI TEXT);")

                    db?.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_DST_LIST} (${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "$COLUMN_TIME_SET INTEGER, " +
                            "$COLUMN_TIME_ZONE TEXT, " +
                            "$COLUMN_ALARM_ID INTEGER);")
                }
            }
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

        fun insertRandom(numberOfItems: Int) {
            writableDatabase.let {
                for(i in 0 until numberOfItems) {
                    val alarm = TestUtils.createAlarm(label = TestUtils.SAMPLE_TEXT)
                    val alarmCv = ContentValues().apply {
                        put(COLUMN_TIME_ZONE, alarm.timeZone)
                        put(COLUMN_TIME_SET, alarm.timeSet)
                        put(COLUMN_REPEAT, alarm.repeat.contentToString())
                        put(COLUMN_RINGTONE, alarm.ringtone)
                        put(COLUMN_VIBRATION, Arrays.toString(alarm.vibration))
                        put(COLUMN_SNOOZE, alarm.snooze)
                        put(COLUMN_LABEL, alarm.label)
                        put(COLUMN_ON_OFF, alarm.on_off)
                        put(COLUMN_NOTI_ID, alarm.notiId)
                        put(COLUMN_COLOR_TAG, alarm.colorTag)
                        put(COLUMN_INDEX, alarm.index)
                        put(COLUMN_START_DATE, alarm.startDate)
                        put(COLUMN_END_DATE, alarm.endDate)

                        if(version > 5) {
                            put(COLUMN_PICKER_TIME, alarm.pickerTime)
                        }
                    }
                    it.insert(TABLE_ALARM_LIST, null, alarmCv)
                }

                close()
            }
        }

        fun insertSampleData() {
            writableDatabase.let {
                val alarm = TestUtils.createAlarm()
                val alarmCv = ContentValues().apply {
                    put(COLUMN_TIME_ZONE, alarm.timeZone)
                    put(COLUMN_TIME_SET, alarm.timeSet)
                    put(COLUMN_REPEAT, alarm.repeat.contentToString())
                    put(COLUMN_RINGTONE, alarm.ringtone)
                    put(COLUMN_VIBRATION, Arrays.toString(alarm.vibration))
                    put(COLUMN_SNOOZE, alarm.snooze)
                    put(COLUMN_LABEL, alarm.label)
                    put(COLUMN_ON_OFF, alarm.on_off)
                    put(COLUMN_NOTI_ID, alarm.notiId)
                    put(COLUMN_COLOR_TAG, alarm.colorTag)
                    put(COLUMN_INDEX, alarm.index)
                    put(COLUMN_START_DATE, alarm.startDate)
                    put(COLUMN_END_DATE, alarm.endDate)

                    if(version > 5) {
                        put(COLUMN_PICKER_TIME, alarm.pickerTime)
                    }
                }
                it.insert(TABLE_ALARM_LIST, null, alarmCv)

                val clock = TestUtils.createClock()
                val clockCv = ContentValues().apply {
                    put(COLUMN_TIME_ZONE, clock.timezone)
                    put(COLUMN_INDEX, clock.index)
                }
                it.insert(TABLE_CLOCK_LIST, null, clockCv)
            }

            close()
        }
    }
}