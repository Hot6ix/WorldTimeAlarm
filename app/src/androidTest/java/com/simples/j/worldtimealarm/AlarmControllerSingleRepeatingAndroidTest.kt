package com.simples.j.worldtimealarm

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.threeten.bp.*
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

// Template, copy this format and paste in the test method
/*
* System date/time:
* Date:
* Time:
* Time zone:
* Day of week:
*/
class AlarmControllerSingleRepeatingAndroidTest {

    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)
    }

    @Test
    fun testSingleRepeatingAlarmMon01() {
        /*
        * System date/time: Any date/time but should be Monday
        * Date: None
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        var time = system
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")
            every { ZonedDateTime.now() } returns time
            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon02() {
        /*
        * System date/time: Any date/time but should be Monday
        * Date: None
        * Time: Any + 1
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        var time = system.plusHours(1)
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed01() {
        /*
        * System date/time: Any date/time but should be Wednesday
        * Date: None
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        var time = system
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")
            every { ZonedDateTime.now() } returns time
            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed02() {
        /*
        * System date/time: Any date/time but should be Wednesday
        * Date: None
        * Time: Any + 1
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        var time = system.plusHours(1)
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0) {
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                    }
                    else {
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                    }
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            if(i == 0)
                assertEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            else
                assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmSun01() {
        /*
        * System date/time: Any date/time but should be Sunday
        * Date: None
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

        var time = system
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")
            every { ZonedDateTime.now() } returns time
            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmSun02() {
        /*
        * System date/time: Any date/time but should be Sunday
        * Date: None
        * Time: Any + 1
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

        var time = system.plusHours(1)
        for(i in 0..9) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}")
            Log.d(C.TAG, "given=${time.toInstant()}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}")
            Log.d(C.TAG, "result=${r.toInstant()}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    private fun createAlarm(timeZone: String = TimeZone.getDefault().id,
                    timeSet: Instant = Instant.now(),
                    repeat: IntArray = IntArray(7) {0},
                    startDate: Long? = null,
                    endDate: Long? = null): AlarmItem {
        val notiId = 100000 + Random().nextInt(899999)
        return AlarmItem(
                null,
                timeZone,
                timeSet.toEpochMilli().toString(),
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
                timeSet.toEpochMilli()
        )
    }
}
