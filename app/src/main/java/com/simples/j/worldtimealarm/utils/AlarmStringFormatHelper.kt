package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

object AlarmStringFormatHelper {

    fun formatDate(context: Context, startDate: ZonedDateTime?, endDate: ZonedDateTime?, hasRepeat: Boolean): String {
        val s = startDate?.withZoneSameLocal(ZoneId.systemDefault())?.toInstant()
        val e = endDate?.withZoneSameLocal(ZoneId.systemDefault())?.toInstant()

        return when {
            s != null && e != null -> {
                DateUtils.formatDateRange(context, s.toEpochMilli(), e.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
            }
            s != null -> {
                if(hasRepeat)
                    context.getString(R.string.range_begin).format(DateUtils.formatDateTime(context, s.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                else
                    DateUtils.formatDateTime(context, s.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL)

            }
            e != null -> {
                context.getString(R.string.range_until).format(DateUtils.formatDateTime(context, e.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
            }
            else -> {
                context.getString(R.string.range_not_set)
            }
        }
    }

    fun getDisplayLocalDate(context: Context, startDate: ZonedDateTime?, endDate: ZonedDateTime?, hasRepeat: Boolean): String? {
        Log.d(C.TAG, "$startDate, $endDate")
        return when {
            startDate != null && endDate != null -> {
                DateUtils.formatDateRange(context, startDate.toInstant().toEpochMilli(), endDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
            }
            startDate != null -> {
                if(hasRepeat) context.getString(R.string.range_begin).format(DateUtils.formatDateTime(context, startDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                else null
            }
            endDate != null -> {
                context.getString(R.string.range_until).format(DateUtils.formatDateTime(context, endDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
            }
            else -> {
                null
            }
        }
    }

    fun getDisplayLocalRepeatArray(context: Context, repeats: IntArray, dateTimeInLocal: ZonedDateTime?, timeZone: String, applyRepeat: Boolean = true): String {
        // for support old version of app
        var repeat = repeats.mapIndexed { index, i ->
            if (i > 0) index + 1 else 0
        }.filter { it != 0 }.map {
            var converted = it - 1
            if (converted == 0) converted = 7

            DayOfWeek.of(converted)
        }

        if(applyRepeat) {
            repeat = repeat.mapNotNull {
                val nextRepeatDateTime = dateTimeInLocal
                        ?.withZoneSameInstant(ZoneId.of(timeZone))
                        ?.with(TemporalAdjusters.nextOrSame(it))

                nextRepeatDateTime?.withZoneSameInstant(ZoneId.systemDefault())?.dayOfWeek
            }
        }

        return if (repeat.size == 7) context.resources.getString(R.string.everyday)
        else if (repeat.contains(DayOfWeek.SATURDAY) && repeat.contains(DayOfWeek.SUNDAY) && repeat.size == 2) context.resources.getString(R.string.weekend)
        else if (repeat.contains(DayOfWeek.MONDAY)
                && repeat.contains(DayOfWeek.TUESDAY)
                && repeat.contains(DayOfWeek.WEDNESDAY)
                && repeat.contains(DayOfWeek.THURSDAY)
                && repeat.contains(DayOfWeek.FRIDAY)
                && repeat.size == 5) context.resources.getString(R.string.weekday)
        else if (repeat.size == 1) {
            repeat[0].getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else {
            repeat.joinToString {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        }
    }

    fun formatRepeat(context: Context, array: IntArray): String {
        // for support old version of app
        val repeat = array.mapIndexed { index, i ->
            if (i > 0) index + 1 else 0
        }.filter { it != 0 }.map {
            var converted = it - 1
            if (converted == 0) converted = 7

            DayOfWeek.of(converted)
        }

        return if (repeat.size == 7) context.resources.getString(R.string.everyday)
        else if (repeat.contains(DayOfWeek.SATURDAY) && repeat.contains(DayOfWeek.SUNDAY) && repeat.size == 2) context.resources.getString(R.string.weekend)
        else if (repeat.contains(DayOfWeek.MONDAY)
                && repeat.contains(DayOfWeek.TUESDAY)
                && repeat.contains(DayOfWeek.WEDNESDAY)
                && repeat.contains(DayOfWeek.THURSDAY)
                && repeat.contains(DayOfWeek.FRIDAY)
                && repeat.size == 5) context.resources.getString(R.string.weekday)
        else if (repeat.size == 1) {
            repeat[0].getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else {
            repeat.joinToString {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        }
    }
}