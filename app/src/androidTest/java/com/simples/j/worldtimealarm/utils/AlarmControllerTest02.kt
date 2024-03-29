package com.simples.j.worldtimealarm.utils

import android.content.Context
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.simples.j.worldtimealarm.etc.AlarmItem
import org.junit.Assert.assertEquals
import org.junit.Before
import java.util.*

@MediumTest
class AlarmControllerTest02 {

    private lateinit var context: Context

    @Before
    fun setup() {
        this.context = InstrumentationRegistry.getInstrumentation().context
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"))
    }

//    @Test
    // old test
    fun test() {
        val a = TimeZone.getDefault()
        assertEquals("Asia/Taipei", a.id)

        val eCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0) }
        val item = createAlarm(timeZone = "Asia/Seoul", repeat = intArrayOf(0,0,0,1,0,0,0), timeSet = eCal)
        val time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        val cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        assertEquals(4, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals("2019/3/27/", calendarToString(cal, false))

        // mock 'Apply time difference to repeat day(s)' option
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
                endDate,
                timeSet.timeInMillis
        )
    }

    private fun calendarToString(cal: Calendar, includeTime: Boolean, separator: String = "/"): String {
        val arr = arrayOf(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR, Calendar.MINUTE)
        val builder = StringBuilder()
        val until =
                if(includeTime) arr.size
                else arr.size - 3

        for(index in 0..until) {
            var value = cal.get(arr[index])
            if(index == 1) value += 1
            builder.append(value)
            builder.append(separator)
        }

        return builder.toString()
    }
}