package com.simples.j.worldtimealarm

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AlarmControllerAndroidTest {

    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)
    }

//    @Test
    fun printDateTimeFormatter() {
        val current = ZonedDateTime.now()
        println("========================")
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
        println("========================")
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)))
        println("========================")
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)))
        println(current.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)))
        println("========================")
        println(current.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        println(current.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        println(current.format(DateTimeFormatter.ISO_DATE_TIME))
        println("========================")
    }

    @Test
    fun printTimeZone() {
        val list = MediaCursor.getTimeZoneLocales()

        list.forEach { uLocale ->
            val tzs = MediaCursor.getTimeZoneListByCountry(uLocale.country)

            tzs.forEach {
                var name = it.mExemplarName
                if(name == null) {
                    name =
                            if(it.mTimeZone.inDaylightTime(Date())) it.mDaylightName
                            else it.mStandardName
                }

                Log.d(C.TAG, "$it")
//                if(it.mTimeZone.dstSavings > 0 && it.mTimeZone.dstSavings != 3600000) {
//                    println("${uLocale.displayCountry}, $name," +
//                            " useDST=${it.mTimeZone.useDaylightTime()}," +
//                            " dstSaving=${it.mTimeZone.dstSavings}," +
//                            " ${it.mTimeZone.inDaylightTime(Date())}")
//                }
            }
        }
    }

    @Test
    fun testSimpleAlarms() {
        // current +1 hour
        val now = ZonedDateTime.now().withSecond(0).withNano(0)
        var timeSet = now.plusHours(1)
        var item = createAlarm(timeSet = timeSet.toInstant())
        var result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        assertTrue(timeSet.isEqual(result))
        assertFalse(now.isEqual(result))

        // current -1 hour
        timeSet = now.minusHours(1)
        item = createAlarm(timeSet = timeSet.toInstant())
        result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)
        var answer = timeSet.plusDays(1)

        assertFalse(timeSet.isEqual(result))
        assertTrue(timeSet.isBefore(result))
        assertTrue(answer.isEqual(result))
    }

    @Test
    fun testDifferentTimeZone() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0)
        var timeSet = now.withMinute(0)
        var item = createAlarm(timeSet = timeSet.toInstant(), timeZone = "Asia/Seoul")
        var result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)
        var answer = timeSet.plusDays(1)

        assertFalse(timeSet.isEqual(result))
        assertTrue(timeSet.isBefore(result))
        assertTrue(answer.isEqual(result))
    }

    @Test
    fun testSingleRepeating() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0)
        val item = createAlarm(timeSet = now.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
        val result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        val answer = ZonedDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        Log.d(C.TAG, "=====================================")
        Log.d(C.TAG, "testSingleRepeating=Epoch(${now.toInstant()}), system(${now.withZoneSameInstant(ZoneId.systemDefault())}) result($result), resultInSystem(${result.withZoneSameInstant(ZoneId.systemDefault())})")
        assertEquals(answer.toInstant().toEpochMilli(), result.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertNotEquals(now.toInstant().toEpochMilli(), result.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testAnHourAfterFromNowAndSingleRepeating() {
        val now = ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0)
        val item = createAlarm(timeSet = now.toInstant(), repeat = intArrayOf(0,0,0,1,0,0,0))
        val result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        val answer = ZonedDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))

        Log.d(C.TAG, "=====================================")
        Log.d(C.TAG, "testAnHourAfterFromNowAndSingleRepeating=Epoch(${now.toInstant()}), system(${now.withZoneSameInstant(ZoneId.systemDefault())}) result($result), resultInSystem(${result.withZoneSameInstant(ZoneId.systemDefault())})")
        assertEquals(answer.toInstant().toEpochMilli(), result.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testMultipleRepeating() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0)
        val item = createAlarm(timeSet = now.toInstant(), repeat = intArrayOf(0,0,0,1,1,1,0))
        val result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        Log.d(C.TAG, "=====================================")
        Log.d(C.TAG, "testMultipleRepeating=Epoch(${now.toInstant()}), system(${now.withZoneSameInstant(ZoneId.systemDefault())}) result($result), resultInSystem(${result.withZoneSameInstant(ZoneId.systemDefault())})")
        assertNotEquals(now.toInstant().toEpochMilli(), result.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testSingleRepeatingWithDifferentTimeZone() {
        // Local Timezone: America/New_York in DST (-04:00)
        // Given: 12:00:00 Asia/Seoul (+09:00), Every Wednesday
        // Should be: Wednesday 12:00:00 Asia/Seoul
        // Should be(local): Tuesday 23:00:00 America/New_York
        val id = "Asia/Seoul"
        val local = LocalDateTime.now(ZoneId.of(id))
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val given = local.atZone(ZoneId.of(id))

        val item = createAlarm(timeSet = given.toInstant(), timeZone = id, repeat = intArrayOf(0,0,0,1,0,0,0))

        val result = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        val answer = local.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atZone(ZoneId.of(id))

        Log.d(C.TAG, "TestResult(testSingleRepeatingWithDifferentTimeZone)=given(${given}), result(${result})")
        assertEquals(result.toInstant().toEpochMilli(), answer.toInstant().toEpochMilli())
        assertNotEquals(given.toInstant().toEpochMilli(), result.toInstant().toEpochMilli())
        assertNotEquals(given.toInstant().toEpochMilli(), answer.toInstant().toEpochMilli())
        assertTrue(answer.withZoneSameInstant(ZoneId.systemDefault()).isEqual(result.withZoneSameInstant(ZoneId.systemDefault())))
    }

    private fun createAlarm(timeZone: String = TimeZone.getDefault().id,
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
