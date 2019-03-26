package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.icu.text.LocaleDisplayNames
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.media.RingtoneManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
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

            val daysInYear =
                    if(calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                        val tmpCal = today.clone() as Calendar
                        var tmpMax = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                        // this loop is for handle leap year
                        while(tmpCal.get(Calendar.YEAR) <= calendar.get(Calendar.YEAR)) {
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

        @RequiresApi(Build.VERSION_CODES.N)
        fun getTimeZoneLocales(): List<ULocale> {
            // filter duplicate and only has timezones
            // TODO : 중복된 로케일이 제거된 경우 인도나 쿠바같은 국가의 도시가 표시가 안되는 경우가 있음
            return ULocale.getAvailableLocales().distinctBy { it.country }.filter { TimeZone.getAvailableIDs(it.country).isNotEmpty() }.sortedBy { it.displayCountry }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getCountryNameByTimeZone(timeZone: TimeZone?, uLocale: ULocale = ULocale.getDefault()): String {
            if(timeZone == null) {
                Log.d(C.TAG, "Given timeZone is empty, return nothing.")
                return ""
            }
            return LocaleDisplayNames.getInstance(uLocale).regionDisplayName(TimeZone.getRegion(timeZone.id))
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getULocaleByTimeZoneId(id: String?): ULocale? {
            return ULocale.getAvailableLocales().find {
                it.displayCountry.toLowerCase() == LocaleDisplayNames.getInstance(ULocale.getDefault()).regionDisplayName(TimeZone.getRegion(id)).toLowerCase()
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getTimeZoneListByCountry(country: String?, uLocale: ULocale = ULocale.getDefault()): List<TimeZoneInfo> {
            val javaTimeZone = java.util.TimeZone.getAvailableIDs()
            val list = ArrayList<TimeZoneInfo>()
            TimeZone.getAvailableIDs(country).forEach {
                if(javaTimeZone.contains(it)) {
                    val timeZone = TimeZone.getTimeZone(it)
                    list.add(TimeZoneInfo.Formatter(uLocale.toLocale(), Date()).format(timeZone))
                }
            }
            return list
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getGmtOffsetString(locale: Locale, timeZone: TimeZone?, now: Date): String {
            if(timeZone == null) return ""
            val gmtFormatter = SimpleDateFormat("ZZZZ").apply {
                this.timeZone = timeZone
            }
            var gmtString = gmtFormatter.format(now)

            val bidiFormatter = BidiFormatter.getInstance()
            val isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL

            gmtString = bidiFormatter.unicodeWrap(gmtString, if(isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            return gmtString
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getBestNameForTimeZone(timeZone: TimeZone?): String {
            if(timeZone == null) return ""

            val timeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), Date()).format(timeZone)

            var name = timeZoneInfo.mExemplarName
            if(name == null) {
                name =
                        if(timeZoneInfo.mTimeZone.inDaylightTime(Date())) timeZoneInfo.mDaylightName
                        else timeZoneInfo.mStandardName
            }
            return name ?: timeZone.id
        }

        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        }

        fun getDayDifference(cal1: Calendar, cal2: Calendar, ignoreTime: Boolean): Long {
            if(cal1.timeInMillis != cal2.timeInMillis && ignoreTime) {
                cal1.set(Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY))
                cal1.set(Calendar.MINUTE, cal2.get(Calendar.MINUTE))
                cal1.set(Calendar.SECOND, cal2.get(Calendar.SECOND))
                cal1.set(Calendar.MILLISECOND, cal2.get(Calendar.MILLISECOND))
            }

            val diff = cal1.timeInMillis - cal2.timeInMillis
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        }
    }

}