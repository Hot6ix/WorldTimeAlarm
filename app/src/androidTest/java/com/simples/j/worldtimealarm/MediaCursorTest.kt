package com.simples.j.worldtimealarm

import android.content.Context
import android.icu.text.TimeZoneNames
import android.icu.util.ULocale
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class MediaCursorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
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
        android.icu.util.TimeZone.getAvailableIDs().forEach {
            val timeZone = android.icu.util.TimeZone.getTimeZone(it)
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

}