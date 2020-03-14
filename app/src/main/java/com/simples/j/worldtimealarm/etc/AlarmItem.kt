package com.simples.j.worldtimealarm.etc

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by j on 26/02/2018.
 *
 */
data class AlarmItem(
        var id: Int?,
        var timeZone: String,
        var timeSet: String,
        var repeat: IntArray,
        var ringtone: String?,
        var vibration: LongArray?,
        var snooze: Long,
        var label: String? = null,
        var on_off: Int,
        var notiId: Int,
        var colorTag: Int,
        var index: Int?,
        var startDate: Long? = null,
        var endDate: Long? = null
) : Parcelable {

    override fun toString(): String {
        return "id=$id, timeZone=$timeZone, timeSet=$timeSet, repeat=${repeat.contentToString()}, ringtone=$ringtone, vibration=$vibration, snooze=$snooze, label=$label, on_off=$on_off, notiId=$notiId, colorTag=$colorTag, index=$index, startDate=$startDate, endDate=$endDate"
    }

    override fun equals(other: Any?): Boolean {
        val item = other as AlarmItem?
        return notiId == item?.notiId
    }

    override fun hashCode() = repeat.contentHashCode()

    constructor(source: Parcel) : this(
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readString().toString(),
            source.readString().toString(),
            source.createIntArray() ?: IntArray(7) { 0 },
            source.readString(),
            source.createLongArray(),
            source.readLong(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readValue(Long::class.java.classLoader) as Long?,
            source.readValue(Long::class.java.classLoader) as Long?
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeString(timeZone)
        writeString(timeSet)
        writeIntArray(repeat)
        writeString(ringtone)
        writeLongArray(vibration)
        writeLong(snooze)
        writeString(label)
        writeInt(on_off)
        writeInt(notiId)
        writeInt(colorTag)
        writeValue(index)
        writeValue(startDate)
        writeValue(endDate)
    }

    fun isInstantAlarm(): Boolean = repeat.all { it == 0 } && endDate == null

    fun hasRepeatDay(): Boolean = repeat.any { it > 0 }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AlarmItem> = object : Parcelable.Creator<AlarmItem> {
            override fun createFromParcel(source: Parcel): AlarmItem = AlarmItem(source)
            override fun newArray(size: Int): Array<AlarmItem?> = arrayOfNulls(size)
        }
    }
}