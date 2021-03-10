package com.simples.j.worldtimealarm.utils

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simples.j.worldtimealarm.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
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
    fun init() {}

    @After
    fun terminate() {
    }

    @Test
    fun migrateSqlite_Room() {
        val sqliteDb = TestDatabase(InstrumentationRegistry.getInstrumentation().targetContext)
//        sqliteDb.insertSampleData()

        helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8)
    }

    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 7).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                AppDatabase::class.java,
                TEST_DB
        ).addMigrations(AppDatabase.MIGRATION_7_8).build().apply {
            openHelper.writableDatabase
            close()
        }

    }

    // create test database has version 7
    inner class TestDatabase(val context: Context): SQLiteOpenHelper(context, TEST_DB, null, 7) {
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_ALARM_LIST} (${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${DatabaseManager.COLUMN_TIME_ZONE} TEXT," +
                    "${DatabaseManager.COLUMN_TIME_SET} TEXT," +
                    "${DatabaseManager.COLUMN_REPEAT} TEXT," +
                    "${DatabaseManager.COLUMN_RINGTONE} TEXT," +
                    "${DatabaseManager.COLUMN_VIBRATION} TEXT," +
                    "${DatabaseManager.COLUMN_SNOOZE} INTEGER," +
                    "${DatabaseManager.COLUMN_LABEL} TEXT," +
                    "${DatabaseManager.COLUMN_ON_OFF} INTEGER," +
                    "${DatabaseManager.COLUMN_NOTI_ID} INTEGER," +
                    "${DatabaseManager.COLUMN_COLOR_TAG} INTEGER," +
                    "${DatabaseManager.COLUMN_INDEX} INTEGER," +
                    "${DatabaseManager.COLUMN_START_DATE} INTEGER," +
                    "${DatabaseManager.COLUMN_END_DATE} INTEGER," +
                    "${DatabaseManager.COLUMN_PICKER_TIME} TEXT);")

            db?.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_CLOCK_LIST} (${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${DatabaseManager.COLUMN_TIME_ZONE} TEXT, " +
                    "${DatabaseManager.COLUMN_INDEX} INTEGER);")

            db?.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_USER_RINGTONE} (${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${DatabaseManager.COLUMN_TITLE} TEXT, " +
                    "${DatabaseManager.COLUMN_URI} TEXT);")

            db?.execSQL("CREATE TABLE IF NOT EXISTS ${DatabaseManager.TABLE_DST_LIST} (${DatabaseManager.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${DatabaseManager.COLUMN_TIME_SET} INTEGER, " +
                    "${DatabaseManager.COLUMN_TIME_ZONE} TEXT, " +
                    "${DatabaseManager.COLUMN_ALARM_ID} INTEGER);")
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

        fun insertSampleData() {
            writableDatabase.let {
                val alarm = TestUtils.createAlarm()
                val alarmCv = ContentValues().apply {
                    put(DatabaseManager.COLUMN_TIME_ZONE, alarm.timeZone)
                    put(DatabaseManager.COLUMN_TIME_SET, alarm.timeSet)
                    put(DatabaseManager.COLUMN_REPEAT, alarm.repeat.contentToString())
                    put(DatabaseManager.COLUMN_RINGTONE, alarm.ringtone)
                    put(DatabaseManager.COLUMN_VIBRATION, Arrays.toString(alarm.vibration))
                    put(DatabaseManager.COLUMN_SNOOZE, alarm.snooze)
                    put(DatabaseManager.COLUMN_LABEL, alarm.label)
                    put(DatabaseManager.COLUMN_ON_OFF, alarm.on_off)
                    put(DatabaseManager.COLUMN_NOTI_ID, alarm.notiId)
                    put(DatabaseManager.COLUMN_COLOR_TAG, alarm.colorTag)
                    put(DatabaseManager.COLUMN_INDEX, alarm.index)
                    put(DatabaseManager.COLUMN_START_DATE, alarm.startDate)
                    put(DatabaseManager.COLUMN_END_DATE, alarm.endDate)
                    put(DatabaseManager.COLUMN_PICKER_TIME, alarm.pickerTime)
                }
                it.insert(DatabaseManager.TABLE_ALARM_LIST, null, alarmCv)

                val clock = TestUtils.createClock()
                val clockCv = ContentValues().apply {
                    put(DatabaseManager.COLUMN_TIME_ZONE, clock.timezone)
                    put(DatabaseManager.COLUMN_INDEX, clock.index)
                }
                it.insert(DatabaseManager.TABLE_CLOCK_LIST, null, clockCv)
            }

            close()
        }

        fun drop() {
            writableDatabase.let {
                it.execSQL("DROP TABLE ${DatabaseManager.TABLE_ALARM_LIST}")
                it.execSQL("DROP TABLE ${DatabaseManager.TABLE_CLOCK_LIST}")
            }

            close()
        }
    }
}