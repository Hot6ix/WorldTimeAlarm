package com.simples.j.worldtimealarm

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AlarmControllerTest {

    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun printTimeZone() {
        val list = MediaCursor.getTimeZoneLocales()

        list.forEach { uLocale ->
            val tzs = MediaCursor.getTimeZoneListByCountry(uLocale.country)

            tzs.forEach {
                var name = it.mExemplarName
                if(name == null) {
                    name =
                            if(it.mTimeZone.inDaylightTime(Date())) it.mDaylightName
                            else it.mStandardName
                }

                if(it.mTimeZone.dstSavings > 0 && it.mTimeZone.dstSavings != 3600000) {
                    println("${uLocale.displayCountry}, $name," +
                            " useDST=${it.mTimeZone.useDaylightTime()}," +
                            " dstSaving=${it.mTimeZone.dstSavings}," +
                            " ${it.mTimeZone.inDaylightTime(Date())}")
                }
            }
        }
    }

    @Test
    fun testCalculateDate() {
        // current + 1 hour
        val cal1 = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, +1)
            set(Calendar.SECOND, 0)
        }
        val item = createAlarm(timeSet = cal1)
        val schedule1 = alarmCtrl.calculateDate(item, TYPE_ALARM, false)

        assertEquals(cal1.timeInMillis, schedule1.timeInMillis)
    }

    @Test
    fun testCalculateDateWithDST() {
        // current + 1 hour
        val cal1 = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, +1)
            set(Calendar.SECOND, 0)
        }
        val item = createAlarm(timeSet = cal1, timeZone = "Asia/Seoul")

        val schedule2 = alarmCtrl.calculateDate(item, TYPE_ALARM, false)

        assertNotEquals(cal1.timeInMillis, schedule2.timeInMillis)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun createAlarm(timeZone: String = TimeZone.getDefault().id,
                    timeSet: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) },
                    repeat: IntArray = IntArray(7) {0},
                    startDate: Long? = null,
                    endDate: Long? = null): AlarmItem {
        val notiId = 100000 + Random().nextInt(899999)
        return AlarmItem(
                null,
                timeZone,
                timeSet.timeInMillis.toString(),
                repeat,
                null,
                null,
                0,
                null,
                1,
                notiId,
                0,
                0,
                startDate,
                endDate
        )
    }
}
