package com.simples.j.worldtimealarm

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
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
    fun testOffsetDifference() {
        val seoul = TimeZone.getTimeZone("Asia/Seoul") // UTC+9
        val hongKong = TimeZone.getTimeZone("Asia/Hong_Kong") // UTC+8
        val losAngeles = TimeZone.getTimeZone("America/Los_Angeles") // UTC-8
        val newYork = TimeZone.getTimeZone("America/New_York") // UTC-5
        val london = TimeZone.getTimeZone("Europe/London") // UTC+0
        val paris = TimeZone.getTimeZone("Europe/Paris") // UTC+1
        val gmt = TimeZone.getTimeZone("GMT") // UTC+0

        // Hong Kong - Seoul
        val difference1 = hongKong.getOffset(System.currentTimeMillis()) - seoul.getOffset(System.currentTimeMillis())
        val result1 = MediaCursor.getOffsetOfDifference(context, difference1, MediaCursor.TYPE_CURRENT)
        assertEquals("1시간 느림", result1)

        // Los Angeles - Seoul
        val difference2 = losAngeles.getOffset(System.currentTimeMillis()) - seoul.getOffset(System.currentTimeMillis())
        val result2 = MediaCursor.getOffsetOfDifference(context, difference2, MediaCursor.TYPE_CURRENT)
        assertEquals("17시간 느림", result2)

        // New York - Seoul
        val difference3 = newYork.getOffset(System.currentTimeMillis()) - seoul.getOffset(System.currentTimeMillis())
        val result3 = MediaCursor.getOffsetOfDifference(context, difference3, MediaCursor.TYPE_CURRENT)
        assertEquals("14시간 느림", result3)

        // London - Paris
        val difference4 = london.getOffset(System.currentTimeMillis()) - paris.getOffset(System.currentTimeMillis())
        val result4 = MediaCursor.getOffsetOfDifference(context, difference4, MediaCursor.TYPE_CURRENT)
        assertEquals("1시간 느림", result4)

        // Paris - Los Angeles
        val difference5 = paris.getOffset(System.currentTimeMillis()) - losAngeles.getOffset(System.currentTimeMillis())
        val result5 = MediaCursor.getOffsetOfDifference(context, difference5, MediaCursor.TYPE_CURRENT)
        assertEquals("9시간 빠름", result5)
    }

    @Test
    fun testRemainTime() {

        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }
        val result1 = MediaCursor.getRemainTime(context, cal)
        assertEquals("1분 ", result1)

        cal.add(Calendar.HOUR_OF_DAY, 1)
        val result2 = MediaCursor.getRemainTime(context, cal)
        assertEquals("1시간 1분 ", result2)
    }

}