package com.simples.j.worldtimealarm

import android.content.Context
import android.icu.util.ULocale
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.simples.j.worldtimealarm.etc.C
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
        val first = MediaCursor.getTimeZoneInfoList(locales[0].country)
        assert(first.isNotEmpty())
        locales.sortedBy { it.getDisplayCountry(ULocale.getDefault()) }.forEach {
            Log.d(C.TAG, it.displayCountry)
            Log.d(C.TAG, MediaCursor.getTimeZoneInfoList(it.country).toString())
        }
    }

}