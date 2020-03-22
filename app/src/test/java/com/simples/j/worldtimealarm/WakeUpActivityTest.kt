package com.simples.j.worldtimealarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.simples.j.worldtimealarm.etc.AlarmItem
import kotlinx.android.synthetic.main.activity_wake_up.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class WakeUpActivityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testIntent() {
        /*
            Test case 01
            -------------------
            timeZone : default
            timeSet : current + 1 hour
            ringtone : null string
            vibration : null
            snooze : 0
            label : null
            colorTag : 0
            startDate : null
            endDate : null
        */

        var intent = Intent(context, WakeUpActivity::class.java).apply {
            val b = Bundle().apply {
                putParcelable(AlarmReceiver.ITEM, createAlarm())
            }
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.OPTIONS, b)
            putExtra(AlarmReceiver.EXPIRED, false)
        }

        ActivityScenario.launch<WakeUpActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity.label.visibility == View.GONE)
                assert(activity.time_zone_clock_layout.visibility == View.GONE)
            }
        }

        // Test case 02 : empty bundle

        intent = Intent(context, WakeUpActivity::class.java).apply {
            val b = Bundle()
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.OPTIONS, b)
            putExtra(AlarmReceiver.EXPIRED, false)
        }

        ActivityScenario.launch<WakeUpActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity.label.visibility == View.VISIBLE)
                assert(activity.time_zone_clock_layout.visibility == View.GONE)
            }
        }

        // Test case 03 : no bundle

        intent = Intent(context, WakeUpActivity::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXPIRED, false)
        }

        ActivityScenario.launch<WakeUpActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity.label.visibility == View.VISIBLE)
                assert(activity.time_zone_clock_layout.visibility == View.GONE)
            }
        }
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
                "null",
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

}