package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    private val round = 10

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)

        mockkStatic(ZoneId::class)
        every { ZoneId.systemDefault() } returns ZoneId.of("UTC")
    }

    @Test
    fun testSingleRepeatingAlarmMon01() {
        /*
        * System date/time: Monday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon02() {
        /*
        * System date/time: Monday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon03() {
        /*
        * System date/time: Monday in May (in DST)
        * Date: None
        * Time: 0800
        * Time zone: New York (GMT-4, DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.MAY.value)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon04() {
        /*
        * System date/time: Monday in May (in DST)
        * Date: None
        * Time: 2100
        * Time zone: New York (GMT-4, DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.MAY.value)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(21).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.THURSDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            assertEquals(1, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon05() {
        /*
        * System date/time: Monday in December (not in DST)
        * Date: None
        * Time: 0800
        * Time zone: New York (GMT-5, not in DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.DECEMBER.value)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon06() {
        /*
        * System date/time: Feb, last Monday
        * Date: From 1 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        var time = system
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0) startDate.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                    else time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon07() {
        /*
        * System date/time: Feb, last Monday
        * Date: 1 Mar - 31 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = system
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer =
                    if(i == 0) startDate.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                    else time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmMon08() {
        /*
        * System date/time: Mar, first Monday
        * Date: 1 Mar - 31 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = system
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer = time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed01() {
        /*
        * System date/time: Wednesday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed02() {
        /*
        * System date/time: Wednesday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

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
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
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
    fun testSingleRepeatingAlarmWed03() {
        /*
        * System date/time: 0800, Wednesday in May (in DST)
        * Date: None
        * Time: 0800
        * Time zone: New York (GMT-4, DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.MAY.value)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0)
                        time.withZoneSameInstant(ZoneId.systemDefault())
                    else
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
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
    fun testSingleRepeatingAlarmWed04() {
        /*
        * System date/time: Wednesday in May (in DST)
        * Date: None
        * Time: 2100
        * Time zone: New York (GMT-4, DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.MAY.value)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(21).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0)
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                            .with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
                    else
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                                .with(TemporalAdjusters.next(DayOfWeek.THURSDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            assertEquals(1, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
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
    fun testSingleRepeatingAlarmWed05() {
        /*
        * System date/time: Monday in December (not in DST)
        * Date: None
        * Time: 0800
        * Time zone: New York (GMT-5, not in DST)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.DECEMBER.value)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0)
                        time.withZoneSameInstant(ZoneId.systemDefault())
                    else
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
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
    fun testSingleRepeatingAlarmWed06() {
        /*
        * System date/time: Feb, last Wednesday
        * Date: From 1 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.WEDNESDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        var time = system
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed07() {
        /*
        * System date/time: Feb, last Wednesday
        * Date: 1 Mar - 31 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.WEDNESDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = system
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer = time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed08() {
        /*
        * System date/time: Mar, first Wednesday
        * Date: 1 Mar - 31 Mar
        * Time: Any
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.WEDNESDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = system
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer = time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed09() {
        /*
        * System date/time: Mar, first Wednesday
        * Date: 1 Mar - 31 Mar
        * Time: Any + 1 hour
        * Time zone: System
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.WEDNESDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = system.plusHours(1)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time.withZoneSameInstant(ZoneId.systemDefault())

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer =
                    if(i == 0) time.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                    else time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")

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
    fun testSingleRepeatingAlarmWed10() {
        /*
        * System date/time: 0800, Feb, last Wednesday (not in DST)
        * Date: 1 Mar - 31 Mar
        * Time: 0800
        * Time zone: New York (in DST during March)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.WEDNESDAY))
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time.withZoneSameInstant(ZoneId.systemDefault())

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz, startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer = time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed11() {
        /*
        * System date/time: 0800, Mar, first Wednesday (not in DST)
        * Date: 1 Mar - 31 Mar
        * Time: 0900
        * Time zone: New York (in DST during March)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.WEDNESDAY))
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(9).withMinute(0)

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())
                .withHour(9).withMinute(0)

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(9).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz, startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            if(r.isEqual(endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) {
                Log.d(C.TAG, "expired")
                break
            }

            val answer =
                    if(i == 0) time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                    else time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                assertEquals(14, r.withZoneSameInstant(ZoneId.systemDefault()).hour)

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmWed12() {
        /*
        * System date/time: 0800, Mar, first Wednesday (not in DST)
        * Date: 1 Mar 2020 - 31 Mar 2021
        * Time: 0900
        * Time zone: New York (in DST during March)
        * Day of week: Wednesday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.WEDNESDAY))
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withYear(2020)
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(9).withMinute(0)

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.lastDayOfMonth())
                .withHour(9).withMinute(0)

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(9).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0), timeZone = tz, startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli())
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer =
                    if(i == 0) time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                    else time.withZoneSameLocal(ZoneId.of(tz)).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

            if((MediaCursor.isSameDay(r, endDate) && r.dayOfWeek != DayOfWeek.WEDNESDAY) || (r.isBefore(answer) && answer.isAfter(endDate))) {
                Log.d(C.TAG, "expired")
                break
            }

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")

            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                assertEquals(14, r.withZoneSameInstant(ZoneId.systemDefault()).hour)

            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmSun01() {
        /*
        * System date/time: Sunday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testSingleRepeatingAlarmSun02() {
        /*
        * System date/time: Sunday
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
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
            val r = alarmCtrl.calculateDateTime(a, TYPE_ALARM)

            val answer = ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                    .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    private fun createAlarm(timeZone: String = ZoneId.systemDefault().id,
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
