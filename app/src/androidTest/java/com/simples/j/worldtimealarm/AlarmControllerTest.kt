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
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AlarmControllerTest {

    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)
    }

//    @Test
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

                if(it.mTimeZone.dstSavings > 0 && it.mTimeZone.dstSavings != 3600000) {
                    println("${uLocale.displayCountry}, $name," +
                            " useDST=${it.mTimeZone.useDaylightTime()}," +
                            " dstSaving=${it.mTimeZone.dstSavings}," +
                            " ${it.mTimeZone.inDaylightTime(Date())}")
                }
            }
        }
    }

    @Test
    fun testAnHourAfterFromNow() {
        // current + 1 hour
        val now = ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0).toInstant()
        val item = createAlarm(timeSet = now)
        val resultOld = alarmCtrl.calculateDate(item, TYPE_ALARM, false).apply {
            set(Calendar.MILLISECOND, 0)
        }
        val resultNew = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        Log.d(C.TAG, "TestResult(testAnHourAfterFromNow)=resultOld(${resultOld.time}), resultNew(${resultNew})")
        assertEquals(now.toEpochMilli(), resultOld.timeInMillis)
        assertEquals(now.toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testDifferentTimeZone() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0).toInstant()
        val item = createAlarm(timeSet = now, timeZone = "Asia/Seoul")
        val resultOld = alarmCtrl.calculateDate(item, TYPE_ALARM, false).apply {
            set(Calendar.MILLISECOND, 0)
        }
        val resultNew = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        Log.d(C.TAG, "TestResult(testDifferentTimeZone)=resultOld(${resultOld.time}), resultNew(${resultNew.withZoneSameInstant(ZoneId.systemDefault())})")
        assertNotEquals(now.toEpochMilli(), resultOld.timeInMillis)
        assertNotEquals(now.toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(resultOld.timeInMillis, resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testSingleRepeating() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0).toInstant()
        val item = createAlarm(timeSet = now, repeat = intArrayOf(0,0,0,1,0,0,0))
        val resultOld = alarmCtrl.calculateDate(item, TYPE_ALARM, false).apply {
            set(Calendar.MILLISECOND, 0)
        }
        val resultNew = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        val answer = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

        Log.d(C.TAG, "TestResult(testSingleRepeating)=resultOld(${resultOld.time}), resultNew(${resultNew})")
        assertEquals(answer.toInstant().toEpochMilli(), resultOld.timeInMillis)
        assertEquals(answer.toInstant().toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertNotEquals(now.toEpochMilli(), resultOld.timeInMillis)
        assertNotEquals(now.toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(resultOld.timeInMillis, resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testAnHourAfterFromNowAndSingleRepeating() {
        val now = ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0).toInstant()
        val item = createAlarm(timeSet = now, repeat = intArrayOf(0,0,0,1,0,0,0))
        val resultOld = alarmCtrl.calculateDate(item, TYPE_ALARM, false).apply {
            set(Calendar.MILLISECOND, 0)
        }
        val resultNew = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        val answer = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))

        Log.d(C.TAG, "TestResult(testAnHourAfterFromNowAndSingleRepeating)=resultOld(${resultOld.time}), resultNew(${resultNew})")
        assertEquals(answer.toInstant().toEpochMilli(), resultOld.timeInMillis)
        assertEquals(answer.toInstant().toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(resultOld.timeInMillis, resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    @Test
    fun testMultipleRepeating() {
        val now = ZonedDateTime.now().withSecond(0).withNano(0).toInstant()
        val item = createAlarm(timeSet = now, repeat = intArrayOf(0,0,0,1,1,1,0))
        val resultOld = alarmCtrl.calculateDate(item, TYPE_ALARM, false).apply {
            set(Calendar.MILLISECOND, 0)
        }
        val resultNew = alarmCtrl.calculateDateTime(item, TYPE_ALARM)

        Log.d(C.TAG, "TestResult(testMultipleRepeating)=resultOld(${resultOld.time}), resultNew(${resultNew})")
        assertNotEquals(now.toEpochMilli(), resultOld.timeInMillis)
        assertNotEquals(now.toEpochMilli(), resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(resultOld.timeInMillis, resultNew.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli())
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
