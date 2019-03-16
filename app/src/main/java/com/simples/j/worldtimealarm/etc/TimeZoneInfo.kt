package com.simples.j.worldtimealarm.etc

import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.os.Build
import android.support.annotation.RequiresApi
import com.simples.j.worldtimealarm.utils.MediaCursor
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneInfo(builder: Builder) {

    val mTimeZone: TimeZone
    val mGenericName: String?
    val mDaylightName: String?
    val mStandardName: String?
    val mExemplarName: String?
    val mGmtOffset: String

    init {
        mTimeZone = builder.timeZone
        mGenericName = builder.mGenericName
        mDaylightName = builder.mDaylightName
        mStandardName = builder.mStandardName
        mExemplarName = builder.mExemplarName
        mGmtOffset = builder.mGmtOffset
    }

    override fun toString(): String {
        return "timeZone : ${mTimeZone.id}, genericName : $mGenericName, daylightName : $mDaylightName, standardName : $mStandardName, exemplarName : $mExemplarName, gmtOffset : $mGmtOffset"
    }

    class Builder(val timeZone: TimeZone) {

        var mGenericName: String? = null
            private set
        var mDaylightName: String? = null
            private set
        var mStandardName: String? = null
            private set
        var mExemplarName: String? = null
            private set
        var mGmtOffset: String = ""
            private set

        fun setGenericName(name: String?) = apply { this.mGenericName = name }

        fun setDayLightName(name: String?) = apply { this.mDaylightName = name }

        fun setStandardName(name: String?) = apply { this.mStandardName = name }

        fun setExemplarName(name: String?) = apply { this.mExemplarName = name }

        fun setGmtOffset(offset: String) = apply { this.mGmtOffset = offset }

        fun build() = TimeZoneInfo(this)
    }

    class Formatter(private val locale: Locale, private val now: Date) {

        fun format(timeZone: TimeZone): TimeZoneInfo {
            val timeZoneNames = TimeZoneNames.getInstance(ULocale.getDefault())
            return TimeZoneInfo.Builder(timeZone)
                    .setGenericName(timeZoneNames.getDisplayName(timeZone.id, TimeZoneNames.NameType.LONG_GENERIC, now.time))
                    .setDayLightName(timeZoneNames.getDisplayName(timeZone.id, TimeZoneNames.NameType.LONG_DAYLIGHT, now.time))
                    .setStandardName(timeZoneNames.getDisplayName(timeZone.id, TimeZoneNames.NameType.LONG_STANDARD, now.time))
                    .setExemplarName(timeZoneNames.getExemplarLocationName(timeZone.id))
                    .setGmtOffset(MediaCursor.getGmtOffsetString(locale, timeZone, now))
                    .build()
        }

    }

}