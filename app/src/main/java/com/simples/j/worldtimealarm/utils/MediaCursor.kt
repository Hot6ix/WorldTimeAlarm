package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.media.RingtoneManager
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * Created by j on 06/03/2018.
 *
 */
class MediaCursor {

    companion object {

        fun getRingtoneList(context: Context): ArrayList<RingtoneItem> {
            val array = ArrayList<RingtoneItem>()
            array.add(RingtoneItem(context.resources.getString(R.string.no_ringtone), null))
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.getRingtone(RingtoneManager.TYPE_ALARM)

            val cursor = ringtoneManager.cursor
            while(cursor.moveToNext()) {
                array.add(RingtoneItem(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), ringtoneManager.getRingtoneUri(cursor.position).toString()))
            }

            return array
        }

        fun getVibratorPatterns(context: Context): ArrayList<PatternItem> {
            val array = ArrayList<PatternItem>()
            array.add(PatternItem(context.resources.getString(R.string.no_vibrator), null))
            val vibrators = context.resources.obtainTypedArray(R.array.vibrator_array)
            val vibratorName = context.resources.getStringArray(R.array.vibrator_name)

            var index = 0
            while(index < vibrators.length()) {
                val pattern = context.resources.getIntArray(vibrators.getResourceId(index, 0))
                val temp = LongArray(pattern.size)
                pattern.forEachIndexed { position, i ->
                    temp[position] = i.toLong()
                }
                array.add(PatternItem(vibratorName[index], temp))
                index++
            }

            vibrators.recycle()
            return array
        }

        fun getOffsetOfDifference(context: Context, difference: Int): String {
            val offsetText = if(difference < 0) context.resources.getString(R.string.slow)
            else context.resources.getString(R.string.fast)

            val hours = TimeUnit.MILLISECONDS.toHours(difference.toLong()).absoluteValue
            val minutes = (TimeUnit.MILLISECONDS.toMinutes(difference.toLong()) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference.toLong()))).absoluteValue

            val offset = when {
                hours > 0 && minutes > 0 -> context.getString(R.string.hours_minutes, hours, minutes) + offsetText // hours & minutes
                hours.toInt() == 1 -> context.getString(R.string.hour, hours) + offsetText // hour
                hours > 0 && minutes.toInt() == 0 -> context.getString(R.string.hours, hours) + offsetText // hours
                hours.toInt() == 0 && minutes > 0 -> context.getString(R.string.minutes, minutes) + offsetText // minutes
                hours.toInt() == 0 && minutes.toInt() == 0 -> context.getString(R.string.same_as_current) // same as current
                else -> ""
            }

            return offset
        }

    }

}