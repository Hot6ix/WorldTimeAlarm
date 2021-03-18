package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.icu.util.TimeZone
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@MediumTest
class AlarmControllerSimpleTest {

    private lateinit var context: Context
    private val alarmCtrl = AlarmController.getInstance()

    private val round = 50

    @Before
    fun setup() {
        this.context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)
    }

    @Test
    fun testTime() {
        mockkStatic(ZonedDateTime::class)
        for(i in 0..round) {
            val randomSystemHour = Random.nextInt(24)
            val system = ZonedDateTime.now()
                    .withHour(randomSystemHour)

            every { ZonedDateTime.now() } returns system

            val randomHour = Random.nextInt(24)
            val timeSet = ZonedDateTime.now().withHour(randomHour)
            val alarm = createAlarm(timeSet = timeSet.toInstant())
            val result = alarmCtrl.calculateDateTime(alarm, TYPE_ALARM)

            val answer =
                    if(system.isBefore(timeSet)) timeSet.withSecond(0).withNano(0)
                    else timeSet.plusDays(1).withSecond(0).withNano(0)

            Assert.assertEquals(answer, result)

            Log.d(C.TAG, "Round=$i")
            Log.d(C.TAG, "system=$system, timeSet=$timeSet")
            Log.d(C.TAG, "answer=$answer, result=$result")
        }
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun testTimeZone() {
        mockkStatic(ZonedDateTime::class)
        for(i in 0..round) {
            val randomSystemHour = Random.nextInt(24)
            val system = ZonedDateTime.now()
                    .withHour(randomSystemHour)

            every { ZonedDateTime.now() } returns system
            
            val randomHour = Random.nextInt(24)
            val randomTimeZone = ZoneId.getAvailableZoneIds().random()
            
            val timeSet = ZonedDateTime.now().withZoneSameInstant(ZoneId.of(randomTimeZone)).withHour(randomHour)
            val alarm = createAlarm(timeSet = timeSet.toInstant(), timeZone = randomTimeZone)
            val result = alarmCtrl.calculateDateTime(alarm, TYPE_ALARM)

            Log.d(C.TAG, "Round=$i")
            Log.d(C.TAG, "system(hour/minute=${system.hour}/${system.minute}, timeZone=${system.zone.id}, inDst=${ZoneId.systemDefault().rules.isDaylightSavings(timeSet.toInstant())})")
            Log.d(C.TAG, "target(hour/minute=${timeSet.hour}/${timeSet.minute}, timeZone=${timeSet.zone.id}, inDst=${ZoneId.of(randomTimeZone).rules.isDaylightSavings(timeSet.toInstant())})")
            Log.d(C.TAG, "diffInHour=${TimeUnit.SECONDS.toHours(system.zone.rules.getOffset(timeSet.toInstant()).totalSeconds.toLong()) - timeSet.zone.rules.getOffset(timeSet.toInstant()).totalSeconds}")
        }
        unmockkStatic(ZonedDateTime::class)
    }

    private fun createAlarm(timeZone: String = ZoneId.systemDefault().id,
                            timeSet: Instant = Instant.now(),
                            repeat: IntArray = IntArray(7) {0},
                            startDate: Long? = null,
                            endDate: Long? = null): AlarmItem {
        val notiId = 100000 + java.util.Random().nextInt(899999)
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