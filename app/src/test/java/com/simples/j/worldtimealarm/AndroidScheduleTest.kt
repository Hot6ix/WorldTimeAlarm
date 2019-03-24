package com.simples.j.worldtimealarm

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class AndroidScheduleTest {

    private lateinit var context: Context
    private lateinit var sharedPref: SharedPreferences

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext()
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Test
    fun test() {
        val a = TimeZone.getDefault()

        val eCal = Calendar.getInstance().apply { set(Calendar.HOUR, 0) }
        val item = createAlarm(timeZone = "Asia/Taipei", repeat = intArrayOf(0,0,0,1,0,0,0), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        var time = AlarmController.getInstance(context).scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        var cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        System.out.println(cal.time.toString())
        Assert.assertEquals(4, cal.get(Calendar.DAY_OF_WEEK))
        Assert.assertEquals("2019/3/27/", calendarToString(cal, false))

        // mock 'Apply time difference to repeat day(s)' option
        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), true).apply()
        time = AlarmController.getInstance(context).scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone affects day repeat
        System.out.println(cal.time.toString())
        Assert.assertEquals(5, cal.get(Calendar.DAY_OF_WEEK))
//        Assert.assertEquals("2019/3/27/", calendarToString(cal, false))
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