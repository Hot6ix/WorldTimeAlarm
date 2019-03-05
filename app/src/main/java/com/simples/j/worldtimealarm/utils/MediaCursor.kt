package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.graphics.Color
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

        const val TYPE_CURRENT = 0
        const val TYPE_CONVERTER = 1

        fun getRingtoneList(context: Context): ArrayList<RingtoneItem> {
            val array = ArrayList<RingtoneItem>()
            array.add(RingtoneItem(context.resources.getString(R.string.no_ringtone), null))
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.getRingtone(RingtoneManager.TYPE_ALARM)

            try {
                val cursor = ringtoneManager.cursor
                while(cursor.moveToNext()) {
                    array.add(RingtoneItem(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), ringtoneManager.getRingtoneUri(cursor.position).toString()))
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
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

        fun getOffsetOfDifference(context: Context, difference: Int, type: Int): String {
            val offsetText = if(difference < 0) context.resources.getString(R.string.slow)
            else context.resources.getString(R.string.fast)

            val hours = TimeUnit.MILLISECONDS.toHours(difference.toLong()).absoluteValue
            val minutes = (TimeUnit.MILLISECONDS.toMinutes(difference.toLong()) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference.toLong()))).absoluteValue

            return when {
                hours > 0 && minutes > 0 -> context.getString(R.string.hours_minutes, hours, minutes) + offsetText // hours & minutes
                hours == 1L && minutes > 0 ->  context.getString(R.string.hour_minutes).format(hours, minutes) + offsetText // hour & minutes
                hours == 1L -> context.getString(R.string.hour, hours) + offsetText // hour
                hours > 0 && minutes.toInt() == 0 -> context.getString(R.string.hours, hours) + offsetText // hours
                hours == 0L && minutes > 0-> context.getString(R.string.minutes, minutes) + offsetText // minutes
                hours == 0L && minutes.toInt() == 0 && type == TYPE_CURRENT -> context.getString(R.string.same_as_current) // same as current
                hours == 0L && minutes.toInt() == 0 && type == TYPE_CONVERTER -> context.getString(R.string.same_as_set) // same as current
                else -> ""
            }
        }

        fun getRemainTime(context: Context, calendar: Calendar): String {
            val today = Calendar.getInstance()
            var difference = calendar.timeInMillis - today.timeInMillis

            val daysInYear = if(calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                val tmpCal = today.clone() as Calendar
                var tmpMax = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                // this loop is for handle leap year
                while(tmpCal.get(Calendar.YEAR) != calendar.get(Calendar.YEAR)) {
                    if(tmpCal.getActualMaximum(Calendar.DAY_OF_YEAR) > tmpMax) {
                        tmpMax = tmpCal.getActualMaximum(Calendar.DAY_OF_YEAR)
                    }
                    tmpCal.add(Calendar.YEAR, 1)
                }
                tmpMax
            }
            else {
                calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
            }

            val years = difference / (daysInYear * 24 * 60 * 60 * 1000L)
            difference %= (daysInYear * 24 * 60 * 60 * 1000L)
            val days = difference / (24 * 60 * 60 * 1000)
            difference %= (24 * 60 * 60 * 1000)

            val hours = difference / (60 * 60 * 1000)
            difference %= (60 * 60 * 1000)
            val minutes = difference / (60 * 1000)
            difference %= (60 * 1000)

            val dateFormat = StringBuilder().apply {
                append(
                        when {
                            years == 1L-> context.getString(R.string.year, years)
                            years > 1 -> context.getString(R.string.years, years)
                            else -> ""
                        }
                )
                append(
                        when {
                            days == 1L -> context.getString(R.string.day, days)
                            days > 1 -> context.getString(R.string.days, days)
                            else -> ""
                        }
                )
                append(
                        when {
                            hours == 1L -> context.getString(R.string.hour, hours)
                            hours > 1 -> context.getString(R.string.hours, hours)
                            else -> ""
                        }
                )
                append(
                        when {
                            minutes == 1L -> context.getString(R.string.minute, minutes)
                            minutes > 1 -> context.getString(R.string.minutes, minutes)
                            else -> {
                                if(this.isEmpty()) {
                                    context.getString(R.string.less_than_a_minute)
                                }
                                else {
                                    ""
                                }
                            }
                        }
                )
            }

            return dateFormat.toString()
        }

        fun makeDarker(color: Int, factor: Float): Int {
            val c = Color.alpha(color)
            val r = Math.round(Color.red(color) * factor)
            val g = Math.round(Color.green(color) * factor)
            val b = Math.round(Color.blue(color) * factor)
            return Color.argb(c, r, g, b)
        }
    }

}