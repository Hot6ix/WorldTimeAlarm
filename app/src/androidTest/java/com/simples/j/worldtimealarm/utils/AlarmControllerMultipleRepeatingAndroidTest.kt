package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.threeten.bp.*
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

class AlarmControllerMultipleRepeatingAndroidTest {

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
    fun testMultipleRepeatingCase01() {
        /*
        * System date/time: Monday
        * Date: None
        * Time: Any
        * Time zone: System
        * Day of week: Friday, Sunday
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

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,0,0,0,0,1,0))
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                if(i == 0 || time.dayOfWeek == DayOfWeek.SUNDAY)
                    ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                else
                    ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase02() {
        /*
        * System date/time: Monday
        * Date: None
        * Time: Any
        * Time zone: System
        * Day of week: Everyday
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
            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time.toInstant()}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,1,1,1,1,1,1))
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer = time.plusDays(1)

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase03() {
        /*
        * System date/time: Monday
        * Date: None
        * Time: Any + 1
        * Time zone: System
        * Day of week: Everyday
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

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time.toInstant()}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,1,1,1,1,1,1))
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0) time
                    else time.plusDays(1)

            Log.d(C.TAG, "answer=${answer.toInstant()}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r.toInstant()}, ${r.dayOfWeek}")
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            if(i == 0)
                Assert.assertEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            else
                Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase04() {
        /*
        * System date/time: Monday in May (in DST)
        * Date: None
        * Time: 0800
        * Time zone: New_York (GMT-4, DST)
        * Day of week: Tuesday, Saturday, Sunday
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

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time.toInstant()}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,0,1,0,0,0,1), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0 || time.dayOfWeek == DayOfWeek.SUNDAY)
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
                    else if(time.dayOfWeek == DayOfWeek.TUESDAY)
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                    else
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}, ${r.withZoneSameInstant(ZoneId.systemDefault()).dayOfWeek}")

            Assert.assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase05() {
        /*
        * System date/time: Monday in May (in DST)
        * Date: None
        * Time: 2100
        * Time zone: New_York (GMT-4, in DST)
        * Day of week: Tuesday, Saturday, Sunday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0)
                .withMonth(Month.MAY.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))

        var time = system.withZoneSameLocal(ZoneId.of(tz)).withHour(21).withMinute(0)
        for(i in 0..round) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "nowInTargetTz=${ZonedDateTime.now().withZoneSameInstant(ZoneId.of(tz))}, ${ZonedDateTime.now().withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}")
            Log.d(C.TAG, "given=${time.toInstant()}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,0,1,0,0,0,1), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    when {
                        i == 0 ->
                            if(system.isAfter(ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.MONDAY))))
                                ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                            else
                                ZonedDateTime.ofInstant(system.withHour(1).withMinute(0).toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                        time.dayOfWeek == DayOfWeek.TUESDAY -> ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                        time.dayOfWeek == DayOfWeek.SUNDAY -> ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                        else -> ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    }

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}, ${r.withZoneSameInstant(ZoneId.systemDefault()).dayOfWeek}")

            Assert.assertEquals(1, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase06() {
        /*
        * System date/time: Monday in December (not in DST)
        * Date: None
        * Time: 0800
        * Time zone: New_York (GMT-5, not in DST)
        * Day of week: Tuesday, Saturday, Sunday
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

            Log.d(C.TAG, "now=${ZonedDateTime.now().toInstant()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time.toInstant()}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,0,1,0,0,0,1), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0 || time.dayOfWeek == DayOfWeek.SUNDAY)
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
                    else if(time.dayOfWeek == DayOfWeek.TUESDAY)
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                    else
                        ZonedDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

            Log.d(C.TAG, "answerTz=${answer.zone.id}, resultTz=${r.zone.id}")
            Log.d(C.TAG, "answer=${answer.withZoneSameInstant(ZoneId.of(tz))}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer}, ${answer.dayOfWeek}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}, ${r.withZoneSameInstant(ZoneId.systemDefault()).dayOfWeek}")

            Assert.assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase07() {
        /*
        * System date/time: Last Monday in Feb
        * Date: 1 Mar - 31 Mar
        * Time: 0800
        * Time zone: New York
        * Day of week: Monday, Wednesday, Saturday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time.withZoneSameInstant(ZoneId.systemDefault())

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,1,0,1,0,0,1), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli(), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0 || time.dayOfWeek == DayOfWeek.SATURDAY)
                        time.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    else if(time.dayOfWeek == DayOfWeek.MONDAY)
                        time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                    else
                        time.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

            if(a.apply { timeSet = r.toInstant().toEpochMilli().toString() }.isExpired()) {
                Log.d(C.TAG, "expired")
                break
            }

            Log.d(C.TAG, "answer=${answer}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")


            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                Assert.assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                Assert.assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase08() {
        /*
        * System date/time: 0700, First Monday in Mar
        * Date: 1 Mar - 31 Mar
        * Time: 0800
        * Time zone: New York
        * Day of week: Everyday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.MARCH.value)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
                .withHour(7).withMinute(0)
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            if(i == 0) every { ZonedDateTime.now() } returns system
            else every { ZonedDateTime.now() } returns time.withZoneSameInstant(ZoneId.systemDefault())

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(1,1,1,1,1,1,1), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli(), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0) time.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    else {
                        when (time.dayOfWeek) {
                            DayOfWeek.MONDAY -> time.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
                            DayOfWeek.TUESDAY -> time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                            DayOfWeek.WEDNESDAY -> time.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
                            DayOfWeek.THURSDAY -> time.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                            DayOfWeek.FRIDAY -> time.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                            DayOfWeek.SATURDAY -> time.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                            DayOfWeek.SUNDAY -> time.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                            else -> throw Exception("Unknown")
                        }
                    }

            if(a.copy(timeSet = r.toInstant().toEpochMilli().toString()).isExpired()) {
                Log.d(C.TAG, "expired")
                break
            }

            Log.d(C.TAG, "answer=${answer}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")


            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                Assert.assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                Assert.assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            time = r
        }

        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testMultipleRepeatingCase09() {
        /*
        * System date/time: Last Monday in Feb
        * Date: 1 Mar 2020 - 31 Mar 2021
        * Time: 0800
        * Time zone: New York
        * Day of week: Monday, Wednesday, Saturday
        */

        mockkStatic(ZonedDateTime::class)

        val tz = "America/New_York"
        val system = ZonedDateTime.now()
                .withMonth(Month.FEBRUARY.value)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
                .withSecond(0)
                .withNano(0)

        val startDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withYear(2020)
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.firstDayOfMonth())

        val endDate = system
                .withZoneSameLocal(ZoneId.of(tz))
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withHour(8).withMinute(0)
                .with(TemporalAdjusters.lastDayOfMonth())

        var time = startDate.withZoneSameLocal(ZoneId.of(tz)).withHour(8).withMinute(0)
        for(i in 0..9999) {
            Log.d(C.TAG, "Round: $i")

            every { ZonedDateTime.now() } returns time.withZoneSameInstant(ZoneId.systemDefault())

            Log.d(C.TAG, "now=${ZonedDateTime.now()}, ${ZonedDateTime.now().dayOfWeek}")
            Log.d(C.TAG, "given=${time}, ${time.dayOfWeek}")

            val a = createAlarm(timeSet = time.toInstant(), repeat = intArrayOf(0,1,0,1,0,0,1), startDate = startDate.toInstant().toEpochMilli(), endDate = endDate.toInstant().toEpochMilli(), timeZone = tz)
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            val answer =
                    if(i == 0 || time.dayOfWeek == DayOfWeek.SATURDAY)
                        time.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    else if(time.dayOfWeek == DayOfWeek.MONDAY)
                        time.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                    else
                        time.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

            if(a.apply { timeSet = r.toInstant().toEpochMilli().toString() }.isExpired()) {
                Log.d(C.TAG, "expired")
                break
            }

            Log.d(C.TAG, "answer=${answer}, ${answer.withZoneSameInstant(ZoneId.of(tz)).dayOfWeek}, inSystemTz=${answer.withZoneSameInstant(ZoneId.systemDefault())}")
            Log.d(C.TAG, "result=${r}, ${r.dayOfWeek}, inSystemTz=${r.withZoneSameInstant(ZoneId.systemDefault())}")


            if(TimeZone.getTimeZone(r.zone.id).inDaylightTime(Date(r.toInstant().toEpochMilli())))
                Assert.assertEquals(12, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            else
                Assert.assertEquals(13, r.withZoneSameInstant(ZoneId.systemDefault()).hour)
            Assert.assertEquals(answer.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

            if(ZonedDateTime.now().year == endDate.year && ZonedDateTime.now().monthValue == endDate.monthValue && ZonedDateTime.now().dayOfMonth == endDate.dayOfMonth)
                Assert.assertEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
            else
                Assert.assertNotEquals(time.toInstant().toEpochMilli(), r.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())

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