package com.simples.j.worldtimealarm

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.simples.j.worldtimealarm.etc.AlarmItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.util.*

@RunWith(RobolectricTestRunner::class)
class AlarmReceiverTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        this.context = getApplicationContext()
    }

    @Test
    fun receiverList() {
        val app: Application = getApplicationContext()
        val shadowApp = Shadows.shadowOf(app)
        assertFalse(shadowApp.registeredReceivers.isEmpty())
    }

    @Test
    fun receiverHandling() {
        val app: Application = getApplicationContext()
        val shadowApp = Shadows.shadowOf(app)

        val nextActivityIntent = Intent(context, WakeUpActivity::class.java)
        var wakeUpActivityIntent = shadowApp.nextStartedActivity

        //
        // Test case 01 : normal
        //
        var intent = Intent(context, AlarmReceiver::class.java).apply {
            val b = Bundle()
            b.putParcelable(AlarmReceiver.ITEM, createAlarm())
            putExtra(AlarmReceiver.OPTIONS, b)
        }
        AlarmReceiver().onReceive(context, intent)
        wakeUpActivityIntent = shadowApp.nextStartedActivity

        assertNotNull("WakeUpActivity started", wakeUpActivityIntent)
        assertEquals("Started service class", wakeUpActivityIntent.component, nextActivityIntent.component)

        //
        // test case 02 : empty bundle
        //
        intent = Intent(context, AlarmReceiver::class.java).apply {
            val b = Bundle()
            putExtra(AlarmReceiver.OPTIONS, b)
        }
        AlarmReceiver().onReceive(context, intent)
        wakeUpActivityIntent = shadowApp.nextStartedActivity

        assertNull("WakeUpActivity should not be started", wakeUpActivityIntent)
        assertNotEquals("Started activity class", wakeUpActivityIntent?.component, nextActivityIntent.component)

        //
        // test case 03 : without bundle
        //
        intent = Intent(context, AlarmReceiver::class.java)
        AlarmReceiver().onReceive(context, intent)
        wakeUpActivityIntent = shadowApp.nextStartedActivity

        assertNull("WakeUpActivity should not be started", wakeUpActivityIntent)
        assertNotEquals("Started activity class", wakeUpActivityIntent?.component, nextActivityIntent.component)

        //
        // test case 04 : wrong type of item with bundle key
        //
        intent = Intent(context, AlarmReceiver::class.java).apply {
            val b = "abc"
            putExtra(AlarmReceiver.OPTIONS, b)
        }
        AlarmReceiver().onReceive(context, intent)
        wakeUpActivityIntent = shadowApp.nextStartedActivity

        assertNull("WakeUpActivity should not be started", wakeUpActivityIntent)
        assertNotEquals("Started activity class", wakeUpActivityIntent?.component, nextActivityIntent.component)

        //
        // test case 06 : alarm item is null
        //
        intent = Intent(context, AlarmReceiver::class.java).apply {
            val b = Bundle()
            b.putParcelable(AlarmReceiver.ITEM, null)
            putExtra(AlarmReceiver.OPTIONS, b)
        }
        AlarmReceiver().onReceive(context, intent)
        wakeUpActivityIntent = shadowApp.nextStartedActivity

        assertNull("WakeUpActivity should not be started", wakeUpActivityIntent)
        assertNotEquals("Started activity class", wakeUpActivityIntent?.component, nextActivityIntent.component)
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
}