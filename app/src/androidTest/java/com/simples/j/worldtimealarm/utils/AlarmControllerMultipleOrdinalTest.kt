package com.simples.j.worldtimealarm.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.TestUtils
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@MediumTest
class AlarmControllerMultipleOrdinalTest {
    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext()
        AndroidThreeTen.init(context)
    }

    @Test
    fun testMultipleCase01() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (7, 1)
            readable ordinals: First Mon-Sun in month
         */

        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val ordinal = arrayOf(
                Pair(1, intArrayOf(1)),
                Pair(2, intArrayOf(1)),
                Pair(3, intArrayOf(1)),
                Pair(4, intArrayOf(1)),
                Pair(5, intArrayOf(1)),
                Pair(6, intArrayOf(1)),
                Pair(7, intArrayOf(1)),
        )

        val target = system
        for(i in 0..10) {
            val a = TestUtils.createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1,2,3,4,5,6,7),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            println(r)
            when(i) {
                0 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.THURSDAY, r.dayOfWeek)
                }
                1 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.FRIDAY, r.dayOfWeek)
                }
                2 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.SATURDAY, r.dayOfWeek)
                }
                3 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.SUNDAY, r.dayOfWeek)
                }
                4 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)
                }
                5 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.TUESDAY, r.dayOfWeek)
                }
                6 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.WEDNESDAY, r.dayOfWeek)
                }
            }

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testMultipleCase02() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 5), (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5)
            readable ordinals: Fifth Mon-Sun in month
         */

        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val ordinal = arrayOf(
                Pair(1, intArrayOf(5)),
                Pair(2, intArrayOf(5)),
                Pair(3, intArrayOf(5)),
                Pair(4, intArrayOf(5)),
                Pair(5, intArrayOf(5)),
                Pair(6, intArrayOf(5)),
                Pair(7, intArrayOf(5)),
        )

        val target = system
        for(i in 0..10) {
            val a = TestUtils.createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1,2,3,4,5,6,7),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            println(r)
            when(i) {
                0 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)
                }
                1 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(DayOfWeek.TUESDAY, r.dayOfWeek)
                }
                2 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(DayOfWeek.WEDNESDAY, r.dayOfWeek)
                }
                3 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.THURSDAY, r.dayOfWeek)
                }
                4 -> {
                    assertEquals(Month.APRIL, r.month)
                    assertEquals(DayOfWeek.FRIDAY, r.dayOfWeek)
                }
                5 -> {
                    assertEquals(Month.MAY, r.month)
                    assertEquals(DayOfWeek.SATURDAY, r.dayOfWeek)
                }
                6 -> {
                    assertEquals(Month.MAY, r.month)
                    assertEquals(DayOfWeek.SUNDAY, r.dayOfWeek)
                }
                7 -> {
                    assertEquals(Month.MAY, r.month)
                    assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)
                }
            }

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testMultipleCase03() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-1-1, 1st Friday (5, 3) 09:00 AM
            start target date/time: 2021-1-1, 1st Friday (5, 3) 09:00 AM
            ordinals: (1, 2), (2, 2), (3, 2)
            readable ordinals: Fifth Mon-Sun in month
         */

        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.JANUARY.value)
                .withDayOfMonth(1)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val ordinal = arrayOf(
                Pair(1, intArrayOf(2)),
                Pair(2, intArrayOf(2)),
                Pair(3, intArrayOf(2)),
        )

        val target = system
        for(i in 0..10) {
            val a = TestUtils.createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1,2,3),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            println(r)
            when(i) {
                0 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(11, r.dayOfMonth)
                    assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)
                }
                1 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(12, r.dayOfMonth)
                    assertEquals(DayOfWeek.TUESDAY, r.dayOfWeek)
                }
                2 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(13, r.dayOfMonth)
                    assertEquals(DayOfWeek.WEDNESDAY, r.dayOfWeek)
                }
                3 -> {
                    assertEquals(Month.FEBRUARY, r.month)
                    assertEquals(8, r.dayOfMonth)
                    assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)
                }
                4 -> {
                    assertEquals(Month.FEBRUARY, r.month)
                    assertEquals(9, r.dayOfMonth)
                    assertEquals(DayOfWeek.TUESDAY, r.dayOfWeek)
                }
                5 -> {
                    assertEquals(Month.FEBRUARY, r.month)
                    assertEquals(10, r.dayOfMonth)
                    assertEquals(DayOfWeek.WEDNESDAY, r.dayOfWeek)
                }
            }

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testMultipleCase04() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2020-12-1, 1st Tuesday (2, 1) 09:00 AM
            start target date/time: 2020-12-1, 1st Tuesday (2, 1) 09:00 AM
            ordinals: (1, 1), (1, 3), (1, 5)
            readable ordinals: First, Third and Fifth Mon in month
         */

        var system = ZonedDateTime.now()
                .withYear(2020)
                .withMonth(Month.DECEMBER.value)
                .withDayOfMonth(1)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val ordinal = arrayOf(
                Pair(1, intArrayOf(1,3,5)),
        )

        val target = system
        for(i in 0..10) {
            val a = TestUtils.createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            println(r)
            when(i) {
                0 -> {
                    assertEquals(Month.DECEMBER, r.month)
                    assertEquals(7, r.dayOfMonth)
                }
                1 -> {
                    assertEquals(Month.DECEMBER, r.month)
                    assertEquals(21, r.dayOfMonth)
                }
                2 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(4, r.dayOfMonth)
                }
                3 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(18, r.dayOfMonth)
                }
                4 -> {
                    assertEquals(Month.FEBRUARY, r.month)
                    assertEquals(1, r.dayOfMonth)
                }
                5 -> {
                    assertEquals(Month.FEBRUARY, r.month)
                    assertEquals(15, r.dayOfMonth)
                }
                6 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(1, r.dayOfMonth)
                }
                7 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(15, r.dayOfMonth)
                }
                8 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(29, r.dayOfMonth)
                }
            }

            assertEquals(DayOfWeek.MONDAY, r.dayOfWeek)

            system = r
            mockAll(system = system)
        }
    }

    private fun mockAll(zoneId: ZoneId = ZoneId.systemDefault(), system: ZonedDateTime = ZonedDateTime.now(), ) {
        unmockkAll()

        mockkStatic(ZoneId::class)
        every { ZoneId.systemDefault() } returns zoneId

        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns system
    }
}