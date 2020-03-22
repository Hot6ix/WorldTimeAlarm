package com.simples.j.worldtimealarm

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import org.hamcrest.CoreMatchers
import org.hamcrest.core.AnyOf
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

        /*
            test case 01
            ---------------
            system time zone : Asia/Seoul
            alarm time zone : Asia/Taipei
            alarm repeat : Wed only
            alarm time (24 hours) : 0
            time diff : 1 hour
        */

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        var eCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0) }
        var item = createAlarm(timeZone = "Asia/Taipei", repeat = intArrayOf(0,0,0,1,0,0,0), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        var time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        var cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        println(cal.time.toString())
        Assert.assertEquals(4, cal.get(Calendar.DAY_OF_WEEK))
        Assert.assertEquals("2019/3/27/", calendarToString(cal, false))

        // enable 'Apply time difference to repeat day(s)' option
        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), true).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone affects day repeat
        println(cal.time.toString())
        Assert.assertEquals(5, cal.get(Calendar.DAY_OF_WEEK))
        Assert.assertEquals("2019/3/28/", calendarToString(cal, false))

        /*
            test case 02
            ---------------
            system time zone : Pacific/Niue
            alarm time zone : Pacific/Kiritimati
            alarm repeat : Tue, Thur
            alarm time (24 hours) : 23
            time diff : 25 hours
        */

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Niue"))
        eCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23) }
        item = createAlarm(timeZone = "Pacific/Kiritimati", repeat = intArrayOf(0,0,3,0,5,0,0), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        println(cal.time.toString())
        Assert.assertThat(cal.get(Calendar.DAY_OF_WEEK), AnyOf.anyOf(CoreMatchers.`is`(3), CoreMatchers.`is`(5)))
        Assert.assertEquals("2019/3/26/", calendarToString(cal, false))

        // enable 'Apply time difference to repeat day(s)' option
        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), true).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone affects day repeat
        println(cal.time.toString())
        Assert.assertThat(cal.get(Calendar.DAY_OF_WEEK), AnyOf.anyOf(CoreMatchers.`is`(1), CoreMatchers.`is`(3)))
        Assert.assertEquals("2019/3/26/", calendarToString(cal, false))

        /*
            test case 03
            ---------------
            system time zone : Asia/Seoul
            alarm time zone : Asia/Seoul
            alarm repeat : Sat, Sun
            alarm time (24 hours) : 23
        */

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        eCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23) }
        item = createAlarm(timeZone = "Asia/Seoul", repeat = intArrayOf(1,0,0,0,0,0,1), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        println(cal.time.toString())
        Assert.assertThat(cal.get(Calendar.DAY_OF_WEEK), AnyOf.anyOf(CoreMatchers.`is`(1), CoreMatchers.`is`(7)))
        Assert.assertEquals("2019/3/30/", calendarToString(cal, false))

        // enable 'Apply time difference to repeat day(s)' option
        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), true).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone affects day repeat
        println(cal.time.toString())
        Assert.assertThat(cal.get(Calendar.DAY_OF_WEEK), AnyOf.anyOf(CoreMatchers.`is`(1), CoreMatchers.`is`(7)))
        Assert.assertEquals("2019/3/30/", calendarToString(cal, false))

        /*
            test case 04
            ---------------
            system time zone : Asia/Seoul
            alarm time zone : Asia/Los_Angeles
            alarm repeat : Mon
            alarm time (24 hours) : 0
            time diff : 16 hours
        */

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        eCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 3) }
        item = createAlarm(timeZone = "Asia/Los_Angeles", repeat = intArrayOf(0,1,0,0,0,0,0), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        println(cal.time.toString())
        Assert.assertEquals(cal.get(Calendar.DAY_OF_WEEK), 2)
        Assert.assertEquals("2019/4/1/", calendarToString(cal, false))

        // enable 'Apply time difference to repeat day(s)' option
        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), true).apply()
        time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone affects day repeat
        println(cal.time.toString())
        Assert.assertEquals(cal.get(Calendar.DAY_OF_WEEK), 3)
        Assert.assertEquals("2019/4/2/", calendarToString(cal, false))
    }

    @Test
    fun testEndDate() {
        /*
            test case 01
            ---------------
            endDate : today + 2 days
            alarm repeat : today
            alarm time (24 hours) : now + 1
            time diff : 1 hour
        */

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        val eCal = Calendar.getInstance().apply { add(Calendar.MINUTE, -1) }
        val item = createAlarm(repeat = intArrayOf(0,0,1,0,0,0,0), timeSet = eCal)

        sharedPref.edit().putBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false).apply()
        val time = AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
        val cal = Calendar.getInstance().apply { timeInMillis = time }

        // time zone doesn't affect day repeat
        println(cal.time.toString())
        Assert.assertEquals(4, cal.get(Calendar.DAY_OF_WEEK))
        Assert.assertEquals("2019/3/27/", calendarToString(cal, false))
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