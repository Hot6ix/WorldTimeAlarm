package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.DayOfWeek
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
class MediaCursorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testRemainTime() {

        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }
        val result1 = MediaCursor.getRemainTime(context, cal)
    }

    @Test
    fun testTimeZone() {
        val locales = MediaCursor.getTimeZoneLocales()
        assert(locales.isNotEmpty())
        Log.d(C.TAG, locales.size.toString())
        val first = MediaCursor.getTimeZoneListByCountry(locales[0].country)
        assert(first.isNotEmpty())

        //
        val list = ArrayList<TimeZoneInfo>()
        val timeZoneNames = TimeZoneNames.getInstance(ULocale.getDefault())
        TimeZone.getAvailableIDs().forEach {
            val timeZone = TimeZone.getTimeZone(it)
            val timeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), Date()).format(timeZone)
            list.add(timeZoneInfo)
        }

        locales.sortedBy { it.getDisplayCountry(ULocale.getDefault()) }.forEach {
            Log.d(C.TAG, it.displayCountry)
            Log.d(C.TAG, MediaCursor.getTimeZoneListByCountry(it.country).toString())
        }

    }

    @Test
    fun testDayDiff() {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.MINUTE, -1)
        cal2.add(Calendar.DAY_OF_MONTH, 6)

        val a = MediaCursor.getDayDifference(cal2, cal1, false)
        assertEquals(a, 5)
        val b = MediaCursor.getDayDifference(cal2, cal1, true)
        assertEquals(b, 6)
    }

    @Test
    fun testDstDiff() {
        val list = getTimeZones()
        list.forEach {
            val diff = MediaCursor.getDstDifference(Date(), java.util.TimeZone.getTimeZone(it.id))

            val tz = TimeZone.getDefault()

            println("${tz.id}, useDST=${tz.useDaylightTime()}, inDST=${tz.inDaylightTime(Date())}")
            println("${it.id}, useDST=${it.useDaylightTime()}, inDST=${it.inDaylightTime(Date())}")
            println("system=${TimeZone.getDefault().id}, given=${it.id}, diffInMinutes=${diff / 1000 / 60}")
        }
    }

    @Test
    fun testTransitions() {
        val i = ZoneId.of(TimeZone.getDefault().id).rules.transitions
        println("$i")
        ZoneId.of(TimeZone.getDefault().id).rules.transitionRules.forEach {
            println("$it")
            println("${it.month}")
            println("${it.dayOfMonthIndicator}")
            println("${it.localTime}")
            println("${it.offsetBefore}")
            println("${it.offsetAfter}")
            println("${it.timeDefinition}")
        }
    }

    @Test
    fun testGetWeekDaysInLocale() {
        val us = MediaCursor.getWeekDaysInLocale(Locale.US)
        assertEquals(DayOfWeek.SUNDAY, us.first())

        val france = MediaCursor.getWeekDaysInLocale(Locale.FRANCE)
        assertEquals(DayOfWeek.MONDAY, france.first())

        val korea = MediaCursor.getWeekDaysInLocale(Locale.KOREA)
        assertEquals(DayOfWeek.SUNDAY, korea.first())

        val uk = MediaCursor.getWeekDaysInLocale(Locale.UK)
        assertEquals(DayOfWeek.MONDAY, uk.first())
    }

    @Test
    fun testRecurrenceArrayUsingLocale() {
        val newYork = "America/New_York"

        val usLocale = MediaCursor.getULocaleByTimeZoneId(newYork)
        usLocale?.let {
            assertEquals("US", it.country)

            val dayOfWeekInLocale = MediaCursor.getWeekDaysInLocale(it.toLocale())
            assertEquals(DayOfWeek.SUNDAY, dayOfWeekInLocale[0])
        }

        val paris = "Europe/Paris"
        val franceLocale = MediaCursor.getULocaleByTimeZoneId(paris)
        franceLocale?.let {
            assertEquals("FR", it.country)

            val dayOfWeekInLocale = MediaCursor.getWeekDaysInLocale(it.toLocale())
            assertEquals(DayOfWeek.MONDAY, dayOfWeekInLocale[0])
        }
    }

    private fun getTimeZones(): ArrayList<TimeZone> {
        val list = MediaCursor.getTimeZoneLocales()
        val tzList = ArrayList<TimeZone>()

        list.forEach { uLocale ->
            val tzs = MediaCursor.getTimeZoneListByCountry(uLocale.country)

            tzs.forEach {
                var name = it.mExemplarName
                if(name == null) {
                    name =
                            if(it.mTimeZone.inDaylightTime(Date())) it.mDaylightName
                            else it.mStandardName
                }

                tzList.add(it.mTimeZone)
            }
        }

        return tzList
    }

}