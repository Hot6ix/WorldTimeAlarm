package com.simples.j.worldtimealarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.jakewharton.threetenabp.AndroidThreeTen
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
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalField
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.collections.ArrayList

@MediumTest
class AndroidOrdinalTest {

    @Before
    fun init() {
        AndroidThreeTen.init(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun testZonedDateTime() {
        val jan = ZonedDateTime.now()
                .withMonth(Month.JANUARY.value)
                .withDayOfMonth(31)

        println(jan)

        val feb = jan.plusMonths(1)
        println(feb)
    }

    @Test
    fun testOrdinal() {
        val now = ZonedDateTime.now()
                .withYear(2021)
                .withMonth(Month.MARCH.value)
                .withDayOfMonth(17)

        assertEquals(3, now.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

        val t1 = ZonedDateTime.now()
                .withMonth(Month.AUGUST.value)
                .withDayOfMonth(1)

        assertEquals(1, t1.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

        val t2 = now.withDayOfMonth(25)
        assertEquals(4, t2.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

        val t3 = now.withDayOfMonth(29)
        assertEquals(5, t3.get(ChronoField.ALIGNED_WEEK_OF_MONTH))

    }

    @Test
    fun testSingleRecurrences() {
        // single ordinal
        val array1 = arrayOf(
                Pair(1, intArrayOf(1)),
        )

        val flatted1 = flatOrdinal(array1)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getAvailableDayOfWeekOrdinal(d, flatted1)

                assertTrue(r.contains(Pair(1, 1)))
            }
        }

        // multiple ordinals
        val array2 = arrayOf(
                Pair(1, intArrayOf(1, 2, 3)),
        )

        val flatted2 = flatOrdinal(array2)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getAvailableDayOfWeekOrdinal(d, flatted2)

                assertTrue(r.contains(Pair(1, 1)) || r.contains(Pair(1, 2)) || r.contains(Pair(1, 3)))
            }
        }

        // multiple ordinals
        val array3 = arrayOf(
                Pair(3, intArrayOf(2, 3)),
        )

        val flatted3 = flatOrdinal(array3)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getAvailableDayOfWeekOrdinal(d, flatted3)

                assertTrue(r.contains(Pair(3, 2)) || r.contains(Pair(3, 3)))
            }
        }
    }

    @Test
    fun testMultiRecurrencesAndMultiOrdinals() {
        val array1 = arrayOf(
                Pair(2, intArrayOf(2,4)),
                Pair(3, intArrayOf(2,5)),
                Pair(4, intArrayOf(2)),
                Pair(5, intArrayOf(1,2)),
                Pair(6, intArrayOf(3,4)),
        )

        val flatted1 = flatOrdinal(array1)

        println("=== Run with given ordinal ===")
        var round = 1
        for(i in 1..5) {
            for(j in 1..7) {
                val d = Pair(j, i)
                val r = getAvailableDayOfWeekOrdinal(d, flatted1)
                val r2 = filterLowestAndHighest(r)
                val r3 = findBest(ZonedDateTime.now(), r2)
                println("===============")
                println("round=$round")
                println("wanted=$flatted1")
                println("given=$d")
                println("available=$r")
                println("filterLowestInOrdinal=$r2")
                println("best=$r3")

                round++
            }
        }

        println("=== Run without given ordinal ===")
        round = 1
        for(i in 1..5) {
            for(j in 1..7) {
                val d = Pair(j, i)
                val r = getAvailableDayOfWeekOrdinal(d, flatted1, true)
                val r2 = filterLowestAndHighest(r)
                val r3 = findBest(ZonedDateTime.now(), r2)
                println("===============")
                println("round=$round")
                println("wanted=$flatted1")
                println("given=$d")
                println("available=$r")
                println("filterLowestInOrdinal=$r2")
                println("best=$r3")

                round++
            }
        }

        val array2 = arrayOf(
                Pair(1, intArrayOf(1,2,3,4,5)),
                Pair(2, intArrayOf(1,2,3,4,5)),
                Pair(3, intArrayOf(1,2,3,4,5)),
                Pair(4, intArrayOf(1,2,3,4,5)),
                Pair(5, intArrayOf(1,2,3,4,5)),
                Pair(6, intArrayOf(1,2,3,4,5)),
                Pair(7, intArrayOf(1,2,3,4,5)),
        )

        val flatted2 = flatOrdinal(array2)

        round = 1
        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getAvailableDayOfWeekOrdinal(d, flatted2)

                assertTrue(r.contains(d))
                round++
            }
        }
    }

    private fun getAvailableDayOfWeekOrdinal(given: Pair<Int, Int>, array: List<Pair<Int, Int>>, ignoreGiven: Boolean = false): List<Pair<Int, Int>> {
        if(array.isEmpty()) return emptyList()

        val list = array.toMutableList()

        if(ignoreGiven) list.remove(given)

        // filter ordinal
        val sameOrAfterWeekOrdinal = list.filter { it.second >= given.second }
        val after = sameOrAfterWeekOrdinal.filterNot { it.first < given.first && it.second == given.second }
        return if(after.isEmpty()) {
            list.filter {
                it.second == list.minByOrNull { o -> o.second }?.second
            }
        }
        else {
            if(after.all { it.second == 5 }) {
                val lowestAvailable = list.filter {
                    it.second == list.minByOrNull { o -> o.second }?.second
                }

                lowestAvailable.toMutableList().apply {
                    addAll(after.sortedBy { it.second })
                }
            }
            else {
                after.sortedBy { it.second }
            }
        }
    }

    private fun filterLowestInOrdinal(array: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        array.groupBy {
            it.second
        }.forEach { (_, u) ->
            u.minByOrNull { it.first }?.let {
                list.add(it)
            }
        }

        return list
    }

    private fun filterLowestAndHighest(array: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        if(array.isEmpty()) return emptyList()

        val list = array.sortedBy { it.second }

        return if(list.size > 1) {
            listOf(list.first(), list.last())
        }
        else listOf(list.first(), list.first())
    }

    private fun findBest(start: ZonedDateTime, array: List<Pair<Int, Int>>): ZonedDateTime? {
        return if(array.isEmpty()) null
        else {
            array.map {
                start.with(TemporalAdjusters.dayOfWeekInMonth(it.second, DayOfWeek.of(it.first)))
            }.filter {
                it.isAfter(start)
            }.minByOrNull {
                it.toInstant().toEpochMilli()
            }
        }
    }

    private fun flatOrdinal(array: Array<Pair<Int, IntArray>>): ArrayList<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        array.forEach { pair ->
            val flatted = pair.second.map {
                Pair(pair.first, it)
            }

            list.addAll(flatted)
        }

        list.sortBy {
            it.second
        }

        return list
    }

    private fun mockAll(zoneId: ZoneId, zonedDateTime: ZonedDateTime) {
        unmockkAll()

        mockkStatic(ZoneId::class)

        every { ZoneId.systemDefault() } returns zoneId

        mockkStatic(ZonedDateTime::class)

        every { ZonedDateTime.now() } returns zonedDateTime
    }

}