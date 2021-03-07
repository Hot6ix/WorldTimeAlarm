package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.icu.util.TimeZone
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simples.j.worldtimealarm.TestUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private val randomTimeZoneList = TimeZone.getAvailableIDs().apply {
        shuffle()
        take(9)
        plus(Calendar.getInstance().timeZone.id)
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testAlarmItemDao() = runBlocking {
        val index = 0
        val alarmItemDao = db.alarmItemDao()
        val item = TestUtils.createAlarm()
        alarmItemDao.insert(item)

        alarmItemDao.getAll().let {
            // check if alarm list is not empty
            assertTrue(it.isNotEmpty())
            assertEquals(1, it.size)
        }
        // delete item
        alarmItemDao.delete(item.notiId)
        // check if alarm list is empty
        assertEquals(0, alarmItemDao.getAll().size)
        // create 10 alarm items
        // 5th and 10th items are disabled
        for(i in 0 until 10) {
            val r = TestUtils.createAlarm()
            if(i % 5 == 0) r.apply { on_off = 0 }
            alarmItemDao.insert(r)
        }
        // check if activated alarm list has 8 items
        assertEquals(8, alarmItemDao.getActivated().size)
        // update first item of activated alarm list disable
        alarmItemDao.update(alarmItemDao.getActivated().first().apply { on_off = 0 })
        // check if activated alarm list has 7 items
        assertEquals(7, alarmItemDao.getActivated().size)
        // delete last item of alarm list
        val lastItem = alarmItemDao.getAll().last()
        alarmItemDao.delete(alarmItemDao.getAll().last())
        assertEquals(alarmItemDao.getAlarmItemFromId(lastItem.id), null)
        // check if random item exist in alarm list using notification id
        val randomItem = alarmItemDao.getAll().random()
        assertNotNull(alarmItemDao.getAlarmItemFromNotificationId(randomItem.notiId))
    }

    @Test
    @Throws(Exception::class)
    fun testClockItemDao() = runBlocking {
        val clockItemDao = db.clockItemDao()
        val item = TestUtils.createClock()
        clockItemDao.insert(item)

        // check if clock item is in clock list
        assertEquals(clockItemDao.getAll().size, 1)

        // check if clock item is deleted and not in clock list
        clockItemDao.delete(item)
        assertTrue(clockItemDao.getAll().find { it == item } == null)
    }

    @Test
    @Throws(Exception::class)
    fun testDstItemDao() {
        // since daylight saving time database is not used, no test code for now.
    }

    @Test
    @Throws(Exception::class)
    fun testRingtoneItemDao() {
        val ringtoneItemDao = db.ringtoneItemDao()
    }

}