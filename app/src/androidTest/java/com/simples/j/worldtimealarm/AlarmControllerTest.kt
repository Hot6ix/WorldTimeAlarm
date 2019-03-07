package com.simples.j.worldtimealarm

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AlarmControllerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        this.context = InstrumentationRegistry.getTargetContext()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        assertEquals("com.simples.j.worldtimealarm", appContext.packageName)
    }

    @Test
    fun testAlarmController() {
        // timeInMillis = the current time as UTC milliseconds from the epoch.
        val alarmCtrl = AlarmController.getInstance(context)
        val scheduledCal = Calendar.getInstance()

        // current + 1 hour
        val cal1 = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, +1)
            set(Calendar.SECOND, 0)
        }
        val item1 = createAlarm(timeSet = cal1)
        val schedule1 = alarmCtrl.scheduleAlarm(context, item1, AlarmController.TYPE_ALARM)

        assertEquals(cal1.timeInMillis, schedule1)

        ////////////////////////////////////////////////////////////////////////////////////////

        // current - 1 hour
        val cal2 = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -1)
            set(Calendar.SECOND, 0)
        }
        val item2 = createAlarm(timeSet = cal2)
        val schedule2 = alarmCtrl.scheduleAlarm(context, item2, AlarmController.TYPE_ALARM)
        scheduledCal.apply {
            timeInMillis = schedule2
        }

        val answer2 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        assert(isSameDay(scheduledCal, answer2))

        ////////////////////////////////////////////////////////////////////////////////////////

        // reserved alarm using startDate
        val cal3 = Calendar.getInstance()
        val start3 = (cal3.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.SECOND, 0)
        }
        val item3 = createAlarm(timeSet = cal3, startDate = start3.timeInMillis)
        val schedule3 = alarmCtrl.scheduleAlarm(context, item3, AlarmController.TYPE_ALARM)
        scheduledCal.apply {
            timeInMillis = schedule3
        }

        // item3 schedule = cal3 + 1 week
        val answer3 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.SECOND, 0)
        }
        assert(isSameDay(scheduledCal, answer3))

        ////////////////////////////////////////////////////////////////////////////////////////

        // repeating alarm - Wednesday only
        val cal4 = Calendar.getInstance()
        val item4 = createAlarm(timeSet = cal4, repeat = intArrayOf(0,0,0,4,0,0,0))
        val schedule4 = alarmCtrl.scheduleAlarm(context, item4, AlarmController.TYPE_ALARM)
        scheduledCal.apply {
            timeInMillis = schedule4
        }

        val answer4 = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, 4)
            set(Calendar.SECOND, 0)
            if(isSameDay(this, scheduledCal)) add(Calendar.WEEK_OF_MONTH, 1)
        }
        assert(isSameDay(scheduledCal, answer4))

        ////////////////////////////////////////////////////////////////////////////////////////
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
