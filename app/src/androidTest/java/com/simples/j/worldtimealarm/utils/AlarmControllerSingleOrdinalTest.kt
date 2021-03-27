package com.simples.j.worldtimealarm.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.TestUtils.createAlarm
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import kotlin.random.Random

@MediumTest
class AlarmControllerSingleOrdinalTest {
    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext()
        AndroidThreeTen.init(context)
    }

    @Test
    fun testSingleCase01() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: First Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0..10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase02() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            start target date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: First Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(1)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0..10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertTrue(r.month == Month.APRIL)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase03() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-1, 1st Monday (1, 1) 08:00 AM
            start target date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: First Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(1)
                .withHour(8)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system.withHour(9)
        for(i in 0..10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertTrue(r.month == Month.MARCH)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase04() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            start target date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            ordinals: (1, 3)
            readable ordinals: Third Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(1)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0..10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(3))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertTrue(r.month == Month.MARCH)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(3, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase05() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            start target date/time: 2021-3-1, 1st Monday (1, 1) 09:00 AM
            ordinals: (1, 5)
            readable ordinals: Third Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(1)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0 until 5) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(5))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            when(i) {
                0 -> {
                    assertEquals(Month.MARCH, r.month)
                    assertEquals(29, r.dayOfMonth)
                }
                1 -> {
                    assertEquals(Month.MAY, r.month)
                    assertEquals(31, r.dayOfMonth)
                }
                2 -> {
                    assertEquals(Month.AUGUST, r.month)
                    assertEquals(30, r.dayOfMonth)
                }
                3 -> {
                    assertEquals(Month.NOVEMBER, r.month)
                    assertEquals(29, r.dayOfMonth)
                }
                4 -> {
                    assertEquals(Month.JANUARY, r.month)
                    assertEquals(31, r.dayOfMonth)
                }
            }

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(5, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase06() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: Third Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0 until 10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertEquals(Month.APRIL, r.month)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCase07() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-2-28, 4th Sunday (7, 4) 09:00 AM
            start target date/time: 2021-2-28, 4th Sunday (7, 4) 09:00 AM
            ordinals: (5, 5)
            readable ordinals: Fifth Fri in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.FEBRUARY.value)
                .withDayOfMonth(28)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val target = system
        for(i in 0 until 10) {
            val ordinal = arrayOf(
                    Pair(5, intArrayOf(5))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(5),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertEquals(Month.APRIL, r.month)

            assertTrue(r.dayOfWeek == DayOfWeek.FRIDAY)
            assertEquals(5, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testRandom() {
        for(i in 0 until 10) {
            val randomYear = Random.nextInt(2000, 2020) // 2000 ~ 2020
            val randomMonth = Random.nextInt(1, 13) // Jan ~ Dec
            val isLeapYear = randomYear % 4 == 0
            val randomDayOfMonth = Random.nextInt(1, Month.of(randomMonth).length(isLeapYear) + 1) // 1 ~ 28-31
            val randomDayOfWeek = Random.nextInt(1, 8) // 1 ~ 7
            val randomOrdinal = Random.nextInt(1, 6) // 1 ~ 5

            val randomZonedDateTime = ZonedDateTime.now()
                    .withYear(randomYear)
                    .withMonth(randomMonth)
                    .withDayOfMonth(randomDayOfMonth)

            val ordinal = arrayOf(
                    Pair(randomDayOfWeek, intArrayOf(randomOrdinal))
            )

            mockAll(system = randomZonedDateTime)

            val a = createAlarm(
                    timeSet = randomZonedDateTime.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = randomZonedDateTime.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            assertEquals(DayOfWeek.of(randomDayOfWeek), r.dayOfWeek)
            assertEquals(randomOrdinal, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))
        }
    }

    @Test
    fun testSingleCaseWithStartDate01() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: Third Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val startDate = ZonedDateTime.now()
                .withMonth(Month.NOVEMBER.value)
                .withDayOfMonth(1)

        val target = system
        for(i in 0 until 10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli(),
                    startDate = startDate.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertEquals(Month.NOVEMBER, r.month)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCaseWithStartDate02() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: Third Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val startDate = ZonedDateTime.now()
                .withYear(2022)
                .withMonth(Month.JANUARY.value)
                .withDayOfMonth(1)

        val target = system
        for(i in 0 until 10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli(),
                    startDate = startDate.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            if(i==0) assertEquals(Month.JANUARY, r.month)

            assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
            assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

            system = r
            mockAll(system = system)
        }
    }

    @Test
    fun testSingleCaseWithEndDate01() {
        /*
            system timezone: UTC
            target timezone: UTC
            start system date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            start target date/time: 2021-3-15, 3rd Monday (1, 3) 09:00 AM
            ordinals: (1, 1)
            readable ordinals: First Mon in month
         */
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(15)
                .withHour(9)
                .withSecond(0)
                .withNano(0)
        mockAll(system = system)

        val endDate = ZonedDateTime.now()
                .withMonth(Month.JUNE.value)
                .withDayOfMonth(30)

        val target = system
        for(i in 0 until 10) {
            val ordinal = arrayOf(
                    Pair(1, intArrayOf(1))
            )

            val a = createAlarm(
                    timeSet = target.toInstant().toString(),
                    repeat = intArrayOf(1),
                    dayOfWeekOrdinal = ordinal,
                    pickerTime = target.toInstant().toEpochMilli(),
                    endDate = endDate.toInstant().toEpochMilli()
            )
            val r = alarmCtrl.calculateDateTime(a, AlarmController.TYPE_ALARM)

            when(i) {
                0 -> assertEquals(Month.APRIL, r.month)
                1 -> assertEquals(Month.MAY, r.month)
                2 -> assertEquals(Month.JUNE, r.month)
            }

            if(i > 2) {
                assertTrue(r.isAfter(endDate) || r.isEqual(endDate))
            }
            else {
                assertTrue(r.dayOfWeek == DayOfWeek.MONDAY)
                assertEquals(1, r.get(ChronoField.ALIGNED_WEEK_OF_MONTH))
            }

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