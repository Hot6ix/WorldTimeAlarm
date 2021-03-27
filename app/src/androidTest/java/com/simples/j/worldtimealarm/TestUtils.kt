package com.simples.j.worldtimealarm

import android.icu.util.TimeZone
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.ClockItem
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object TestUtils {
    private val atomicId = AtomicInteger()
    private val atomicIndex = AtomicInteger()

    fun createAlarm(
            id: Int = atomicId.getAndIncrement(),
            timeZone: String = Calendar.getInstance().timeZone.id,
            timeSet: String = Calendar.getInstance().timeInMillis.toString(),
            repeat: IntArray = intArrayOf(0,0,0,0,0,0,0),
            vibration: LongArray? = null,
            index: Int = atomicIndex.getAndIncrement(),
            startDate: Long? = null,
            endDate: Long? = null,
            pickerTime: Long = Calendar.getInstance().timeInMillis,
            dayOfWeekOrdinal: Array<Pair<Int, IntArray>>? = null
    ): AlarmItem {
        return AlarmItem(
                id = id,
                timeZone = timeZone,
                timeSet = timeSet,
                repeat = repeat,
                ringtone = null,
                vibration = vibration,
                snooze = 0,
                label = null,
                on_off = 1,
                notiId = Random().nextInt(),
                colorTag = -1,
                index = index,
                startDate = startDate,
                endDate = endDate,
                pickerTime = pickerTime,
                dayOfWeekOrdinal = dayOfWeekOrdinal
        )
    }

    fun createClock(
            id: Int = atomicId.getAndIncrement(),
            timeZone: String = Calendar.getInstance().timeZone.id,
            index: Int = atomicIndex.getAndIncrement()
    ): ClockItem {
        return ClockItem(
                id = id,
                timezone = timeZone,
                index = index
        )
    }
}