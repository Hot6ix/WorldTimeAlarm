package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.icu.text.LocaleDisplayNames
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.media.RingtoneManager
import android.os.Build
import android.text.*
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.google.ads.consent.ConsentForm
import com.google.ads.consent.ConsentFormListener
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
import java.net.MalformedURLException
import java.net.URL
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
            array.add(RingtoneItem(title = context.resources.getString(R.string.no_ringtone), uri = ""))
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.getRingtone(RingtoneManager.TYPE_ALARM)

            try {
                val cursor = ringtoneManager.cursor
                while (cursor.moveToNext()) {
                    array.add(RingtoneItem(title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), uri = ringtoneManager.getRingtoneUri(cursor.position).toString()))
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
            val vibratorTitle = context.resources.getStringArray(R.array.vibrator_name)

            var index = 0
            while (index < vibrators.length()) {
                val pattern = context.resources.getIntArray(vibrators.getResourceId(index, 0))
                val temp = LongArray(pattern.size)
                pattern.forEachIndexed { position, i ->
                    temp[position] = i.toLong()
                }
                array.add(PatternItem(vibratorTitle[index], temp))
                index++
            }

            vibrators.recycle()
            return array
        }

        fun getSnoozeList(context: Context): ArrayList<SnoozeItem> {
            val snoozeValues = context.resources.getIntArray(R.array.snooze_values)
            val snoozeTitle = context.resources.getStringArray(R.array.snooze_array)

            val array = snoozeTitle.mapIndexed { index, s ->
                SnoozeItem(s, snoozeValues[index].toLong())
            }

            return ArrayList(array)
        }

        fun getOffsetOfDifference(context: Context, difference: Int, type: Int): String {
            val offsetText = if (difference < 0) context.resources.getString(R.string.slow)
            else context.resources.getString(R.string.fast)

            val hours = TimeUnit.MILLISECONDS.toHours(difference.toLong()).absoluteValue
            val minutes = (TimeUnit.MILLISECONDS.toMinutes(difference.toLong()) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference.toLong()))).absoluteValue

            return when {
                hours > 0 && minutes > 0 -> context.getString(R.string.hours_minutes, hours, minutes) + offsetText // hours & minutes
                hours == 1L && minutes > 0 -> context.getString(R.string.hour_minutes).format(hours, minutes) + offsetText // hour & minutes
                hours == 1L -> context.getString(R.string.hour, hours) + offsetText // hour
                hours > 0 && minutes.toInt() == 0 -> context.getString(R.string.hours, hours) + offsetText // hours
                hours == 0L && minutes > 0 -> context.getString(R.string.minutes, minutes) + offsetText // minutes
                hours == 0L && minutes.toInt() == 0 && type == TYPE_CURRENT -> context.getString(R.string.same_as_current) // same as current
                hours == 0L && minutes.toInt() == 0 && type == TYPE_CONVERTER -> context.getString(R.string.same_as_set) // same as current
                else -> ""
            }
        }

        fun getRemainTime(context: Context, calendar: Calendar): String {
            val today = Calendar.getInstance()
            var difference = calendar.timeInMillis - today.timeInMillis

            val daysInYear =
                    if (calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                        val tmpCal = today.clone() as Calendar
                        var tmpMax = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                        // this loop is for handle leap year
                        while (tmpCal.get(Calendar.YEAR) <= calendar.get(Calendar.YEAR)) {
                            if (tmpCal.getActualMaximum(Calendar.DAY_OF_YEAR) > tmpMax) {
                                tmpMax = tmpCal.getActualMaximum(Calendar.DAY_OF_YEAR)
                            }
                            tmpCal.add(Calendar.YEAR, 1)
                        }
                        tmpMax
                    } else {
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
                            years == 1L -> context.getString(R.string.year, years)
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
                                if (this.isEmpty()) {
                                    context.getString(R.string.less_than_a_minute)
                                } else {
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
            if (timeZone == null) {
                Log.d(C.TAG, "Given timeZone is empty, return nothing.")
                return ""
            }
            return LocaleDisplayNames.getInstance(uLocale).regionDisplayName(TimeZone.getRegion(timeZone.id))
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getULocaleByTimeZoneId(id: String?): ULocale? {
            return ULocale.getAvailableLocales().find {
                it.displayCountry.equals(LocaleDisplayNames.getInstance(ULocale.getDefault()).regionDisplayName(TimeZone.getRegion(id)), ignoreCase = true)
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getTimeZoneListByCountry(country: String?, uLocale: ULocale = ULocale.getDefault()): List<TimeZoneInfo> {
            val javaTimeZone = java.util.TimeZone.getAvailableIDs()
            val list = ArrayList<TimeZoneInfo>()
            TimeZone.getAvailableIDs(country).forEach {
                if (javaTimeZone.contains(it)) {
                    val timeZone = TimeZone.getTimeZone(it)
                    val timeZoneInfo = TimeZoneInfo.Formatter(uLocale.toLocale(), Date()).format(timeZone)

//                    if(timeZoneInfo.hasExtraNames())
                    list.add(timeZoneInfo)
                }
            }
            return list
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getGmtOffsetString(locale: Locale, timeZone: TimeZone?, now: Date): String {
            if (timeZone == null) return ""
            val gmtFormatter = SimpleDateFormat("ZZZZ", Locale.getDefault()).apply {
                this.timeZone = timeZone
            }
            var gmtString = gmtFormatter.format(now)

            val bidiFormatter = BidiFormatter.getInstance()
            val isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL

            gmtString = bidiFormatter.unicodeWrap(gmtString, if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            return gmtString
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getBestNameForTimeZone(timeZone: TimeZone?): String {
            if (timeZone == null) return ""

            val timeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), Date()).format(timeZone)

            var name = timeZoneInfo.mExemplarName
            if (name == null) {
                name =
                        if (timeZoneInfo.mTimeZone.inDaylightTime(Date())) timeZoneInfo.mDaylightName
                        else timeZoneInfo.mStandardName
            }
            return name ?: timeZone.id
        }

        fun isSameDay(from: ZonedDateTime, to: ZonedDateTime): Boolean {
            return from.year == to.year && from.monthValue == to.monthValue && from.dayOfMonth == to.dayOfMonth
        }

        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        }

        fun getDayDifference(from: ZonedDateTime, to: ZonedDateTime): Long {
            val diff = from.toInstant().toEpochMilli() - to.toInstant().toEpochMilli()
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        }

        fun getDayDifference(cal1: Calendar, cal2: Calendar, ignoreTime: Boolean): Long {
            if (cal1.timeInMillis != cal2.timeInMillis && ignoreTime) {
                cal1.set(Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY))
                cal1.set(Calendar.MINUTE, cal2.get(Calendar.MINUTE))
                cal1.set(Calendar.SECOND, cal2.get(Calendar.SECOND))
                cal1.set(Calendar.MILLISECOND, cal2.get(Calendar.MILLISECOND))
            }

            val diff = cal1.timeInMillis - cal2.timeInMillis
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        }

        fun getDstDifference(time: Date, timeZone: java.util.TimeZone): Int {
            var diff = 0
            val systemTimeZone = java.util.TimeZone.getDefault()
            if (systemTimeZone.useDaylightTime() && timeZone.useDaylightTime()) {
                if (systemTimeZone.inDaylightTime(time) && timeZone.inDaylightTime(time)) { // both system and item time zone are in dst
                    diff = systemTimeZone.dstSavings - timeZone.dstSavings
                } else if (systemTimeZone.inDaylightTime(time) && !timeZone.inDaylightTime(time)) { // only system time zone is in dst
                    diff = systemTimeZone.dstSavings
                } else if (!systemTimeZone.inDaylightTime(time) && timeZone.inDaylightTime(time)) { // only item time zone is in dst
                    diff = -timeZone.dstSavings
                }
            } else if (systemTimeZone.useDaylightTime() && !timeZone.useDaylightTime()) {
                diff = systemTimeZone.dstSavings
            } else if (!systemTimeZone.useDaylightTime() && timeZone.useDaylightTime()) {
                diff = -timeZone.dstSavings
            }

            return diff
        }

        fun getLocalizedDateTimeFormat(in24Hour: Boolean = false): String {
            val skeleton =
                if(in24Hour) "yyyyMMMEEEEddHm"
                else "yyyyMMMddEEEEhma"

            return DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
        }

        fun getLocalizedTimeFormat(in24Hour: Boolean = false): SpannableString {
            val skeleton =
                if(in24Hour) "Hm"
                else "hma"

            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton).replace(" ", "\u200A")
            val amPmPos = pattern.indexOf("a")

            val spannable = SpannableString(pattern)
            if(amPmPos >= 0) spannable.setSpan(RelativeSizeSpan(0.5f), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            return spannable
        }

        fun getConsentForm(context: Context?, listener: ConsentFormListener? = null): ConsentForm {
            val url =
                    try {
                        URL("https://github.com/Hot6ix/WorldTimeAlarm/blob/master/privacy_policy.md")
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                        null
                    }

            return ConsentForm.Builder(context, url)
                    .withPersonalizedAdsOption()
                    .withNonPersonalizedAdsOption()
                    .withListener(listener)
                    .build()
        }

        fun getWeekDaysInLocale(locale: Locale = Locale.getDefault()): List<DayOfWeek> {
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val weekDays = DayOfWeek.values()
            val weekDaysInLocale = ArrayList<DayOfWeek>()

            var dayOfWeekValue = weekDays[weekDays.indexOf(firstDayOfWeek)].value
            for (i in weekDays.indices) {
                weekDaysInLocale.add(DayOfWeek.of(dayOfWeekValue))

                dayOfWeekValue++
                if (dayOfWeekValue > 7) dayOfWeekValue = 1
            }

            return weekDaysInLocale
        }

        fun getDayOfWeekValueFromCalendarToThreeTenBp(old: Int): Int {
            var converted = old - 1
            if (converted == 0) converted = 7

            return converted
        }

        fun getAvailableDayOfWeekOrdinal(given: Pair<Int, Int>, array: List<Pair<Int, Int>>, ignoreGiven: Boolean = false): List<Pair<Int, Int>> {
            if (array.isEmpty()) return emptyList()

            val list = array.toMutableList()

            if (ignoreGiven) list.remove(given)

            // filter ordinal
            val sameOrAfterWeekOrdinal = list.filter { it.second >= given.second }
            return if (sameOrAfterWeekOrdinal.isEmpty()) {
                array.filter {
                    it.second == array.minByOrNull { o -> o.second }?.second
                }
            } else {
                if (sameOrAfterWeekOrdinal.all { it.second == 5 }) {
                    val lowestAvailable = array.filter {
                        it.second == array.minByOrNull { o -> o.second }?.second
                    }

                    lowestAvailable.toMutableList().apply {
                        addAll(sameOrAfterWeekOrdinal.sortedBy { it.second })
                    }.distinct()
                } else {
                    sameOrAfterWeekOrdinal.sortedBy { it.second }
                }
            }
        }

        fun filterLowestInOrdinal(array: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
            val list = ArrayList<Pair<Int, Int>>()
            array.groupBy {
                it.second
            }.forEach { (_, u) ->
                u.minByOrNull { it.first }?.let {
                    list.add(it)
                }
            }

            return list
        }

        fun filterLowestAndHighest(array: List<Pair<Int, Int>>): List<Pair<Int, Int>>? {
            if (array.isEmpty()) return null

            val list = array.sortedBy { it.second }

            return if (list.size > 1) {
                listOf(list.first(), list.last())
            } else listOf(list.first(), list.first())
        }

        fun flatOrdinal(array: Array<Pair<Int, IntArray>>): ArrayList<Pair<Int, Int>> {
            val list = ArrayList<Pair<Int, Int>>()
            array.forEach { pair ->
                val flatted = pair.second.map {
                    Pair(pair.first, it)
                }

                list.addAll(flatted)
            }

            list.sortBy {
                it.second
            }

            return list
        }

        fun findBest(atLeast: ZonedDateTime, start: ZonedDateTime, array: List<Pair<Int, Int>>): ZonedDateTime {
            if (array.isEmpty()) throw Exception("list is empty")

            var date = start

            val result = array.map {
                Pair(it.second, start.with(TemporalAdjusters.dayOfWeekInMonth(it.second, DayOfWeek.of(it.first))))
            }.filter {
                it.second.isAfter(atLeast) && it.second.get(ChronoField.ALIGNED_WEEK_OF_MONTH) == it.first
            }.minByOrNull {
                it.second.toInstant().toEpochMilli()
            }

            return if (result == null) {
                date = date.plusMonths(1)
                findBest(atLeast, date, array)
            } else {
                result.second
            }
        }
    }
}