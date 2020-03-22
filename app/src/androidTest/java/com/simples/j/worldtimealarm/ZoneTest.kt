package com.simples.j.worldtimealarm

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.C
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class ZoneTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)
    }

    @Test
    fun printTimeZoneRule() {
        val a = ZoneId.of("America/New_York").rules.nextTransition(Instant.now())
        Log.d(C.TAG, "$a")
        Log.d(C.TAG, "${a.dateTimeAfter}")
        Log.d(C.TAG, "${a.dateTimeBefore}")
        Log.d(C.TAG, "${a.duration}")
        Log.d(C.TAG, "${a.offsetAfter}")
        Log.d(C.TAG, "${a.offsetBefore}")

        val newYork = ZoneId.of("America/New_York")
        val seoul = ZoneId.of("Asia/Seoul")
        val b = newYork.rules.getOffset(Instant.now()).compareTo(seoul.rules.getOffset(Instant.now()))
        Log.d(C.TAG, "$b")

        val dst1 = ZonedDateTime.of(2020, 11, 1, 1, 0, 0, 0, ZoneId.systemDefault())
        val dst2 = ZonedDateTime.of(2020, 11, 1, 2, 0, 0, 0, ZoneId.systemDefault())
        val c = newYork.rules.getOffset(dst1.toInstant()).compareTo(seoul.rules.getOffset(dst1.toInstant()))
        val d = newYork.rules.getOffset(dst2.toInstant()).compareTo(seoul.rules.getOffset(dst2.toInstant()))
        Log.d(C.TAG, "$c, $d")

        val dst3 = ZonedDateTime.of(2020, 11, 1, 6, 0, 0, 0, ZoneId.systemDefault())
    }

}