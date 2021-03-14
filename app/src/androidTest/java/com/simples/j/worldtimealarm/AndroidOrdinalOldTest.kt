package com.simples.j.worldtimealarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.common.util.ArrayUtils
import com.jakewharton.threetenabp.AndroidThreeTen
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
import java.util.*

@RunWith(AndroidJUnit4::class)
@MediumTest
class AndroidOrdinalOldTest {

    @Before
    fun init() {
        AndroidThreeTen.init(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun testGetOrdinalDayOfWeek() {
        println(ZonedDateTime.now())
        // get every second and fourth Thursday of this month from now
        val secondThursday = ZonedDateTime.now().with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.THURSDAY))
        println(secondThursday)
        assertEquals(DayOfWeek.THURSDAY, secondThursday.dayOfWeek)
    }

    @Test
    fun testSingleRecurrenceAndSingleOrdinalInDifferentDay() {
        // Before case
        println("testSingleRecurrenceAndSingleOrdinalInDifferentDay() - Before case")
        var system = ZonedDateTime.now()
                        .withYear(2021)
                        .withMonth(Month.MARCH.value)
                        .withDayOfMonth(1)
                        .withHour(20)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)

        mockAll(ZoneId.of("GMT"), system)

        println("now=${ZonedDateTime.now()}")

        // Saturday, 13 March 2021 09:00 PM GMT
        var time = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(21)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        // Third Monday
        val ordinals = arrayOf(
                Pair(DayOfWeek.MONDAY.value, intArrayOf(5))
        )

        println("alarm time=${time}")
        time = getTime(time, ordinals)
        println("alarm time=${time}")

        assertTrue(time.isAfter(system))
        assertEquals(5, time.get(WeekFields.of(Locale.getDefault()).weekOfMonth()))
        assertEquals(DayOfWeek.MONDAY, time.dayOfWeek)

        // After case
        println("testSingleRecurrenceAndSingleOrdinalInDifferentDay() - After case")
        system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        mockAll(ZoneId.of("GMT"), system)

        println("now=${ZonedDateTime.now()}")

        // Saturday, 13 March 2021 09:00 PM GMT
        time = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(21)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        // oridnals are same
        println("alarm time=${time}")
        time = getTime(time, ordinals)
        println("alarm time=${time}")

        assertTrue(time.isAfter(system))
        assertEquals(5, time.get(WeekFields.of(Locale.getDefault()).weekOfMonth()))
        assertEquals(DayOfWeek.MONDAY, time.dayOfWeek)
    }

    @Test
    fun testSingleRecurrenceAndSingleOrdinalInSameDay() {
        println("testSingleRecurrenceAndSingleOrdinalInSameDay() - Before case")
        var system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(20)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        mockAll(ZoneId.of("GMT"), system)

        println("now=${ZonedDateTime.now()}")

        // Saturday, 13 March 2021 09:00 PM GMT
        var time = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(21)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        // Second Saturday
        val ordinals = arrayOf(
            Pair(DayOfWeek.SATURDAY.value, intArrayOf(2))
        )

        // oridnals are same
        println("alarm time=${time}")
        time = getTime(time, ordinals)
        println("alarm time=${time}")

        assertTrue(time.isAfter(system))
        assertEquals(2, time.get(WeekFields.of(Locale.getDefault()).weekOfMonth()))
        assertEquals(DayOfWeek.SATURDAY, time.dayOfWeek)

        println("testSingleRecurrenceAndSingleOrdinalInSameDay() - After case")
        system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(22)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        mockAll(ZoneId.of("GMT"), system)

        println("now=${ZonedDateTime.now()}")

        time = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(21)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        println("alarm time=${time}")
        time = getTime(time, ordinals)
        println("alarm time=${time}")

        assertTrue(time.isAfter(system))
        assertEquals(2, time.get(WeekFields.of(Locale.getDefault()).weekOfMonth()))
        assertEquals(DayOfWeek.SATURDAY, time.dayOfWeek)
    }

    @Test
    fun testMultipleRecurrencesAndSingleOrdinalInSameDay() {

        val system = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(20)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        mockAll(ZoneId.of("GMT"), system)

        println("now=${ZonedDateTime.now()}")

        // Saturday, 13 March 2021 09:00 PM GMT
        var time = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(13)
                .withHour(21)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        // Second Saturday
        val ordinals = arrayOf(
            Pair(DayOfWeek.SATURDAY.value, intArrayOf(2)),
            Pair(DayOfWeek.MONDAY.value, intArrayOf(2))
        )

        println("alarm time=${time}")
        println("ordinals=${ordinals.joinToString()}")

        time = getTime(time, ordinals)

        println("alarm time=${time}")
        assertTrue(time.isAfter(system))
        assertEquals(2, time.get(WeekFields.of(Locale.getDefault()).weekOfMonth()))
        assertEquals(DayOfWeek.SATURDAY, time.dayOfWeek)
    }

    private fun getTime(timeSet: ZonedDateTime, ordinals: Array<Pair<Int, IntArray>>): ZonedDateTime {
        var time = timeSet

        var isAvailable = false
        while (!isAvailable) {
            var i = 0
            while (i < ordinals.size) {
                val ordinalSet: Pair<Int, IntArray> = ordinals[i]
                if (ordinalSet.first == time.dayOfWeek.value) {
                    val ordinalValues = ordinalSet.second
                    val ordinal = time.get(WeekFields.of(Locale.getDefault()).weekOfMonth())
                    isAvailable = ArrayUtils.contains(ordinalValues, ordinal)
                    if (isAvailable) break
                }

                var ordinalSetIndex = i++
                if (ordinalSetIndex >= ordinals.size) ordinalSetIndex = 0

                val nextOrdinalSet: Pair<Int, IntArray> = ordinals[ordinalSetIndex]
                time = time.with(TemporalAdjusters.next(DayOfWeek.of(nextOrdinalSet.first)))
                i++
            }
        }

        return time
    }

    private fun mockAll(zoneId: ZoneId, zonedDateTime: ZonedDateTime) {
        unmockkAll()

        mockkStatic(ZoneId::class)

        every { ZoneId.systemDefault() } returns zoneId

        mockkStatic(ZonedDateTime::class)

        every { ZonedDateTime.now() } returns zonedDateTime
    }
}