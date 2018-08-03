package com.simples.j.worldtimealarm.etc

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.*

/**
 * Created by j on 26/02/2018.
 *
 */
data class AlarmItem(var id: Int?, var timeZone: String, var timeSet: String, var repeat: IntArray, var ringtone: String?, var vibration: LongArray?, var snooze: Long, var label: String?, var on_off: Int, var notiId: Int, var colorTag: Int, var index: Int?) : Parcelable {
    override fun toString(): String {
        return "$id, $timeZone, $timeSet, ${Arrays.toString(repeat)}, $ringtone, $vibration, $snooze, $label, $on_off, $notiId, $colorTag, $index"
    }

    override fun equals(other: Any?): Boolean {
        val item = other as AlarmItem
        return Arrays.equals(item.repeat, repeat)
    }

    override fun hashCode() = Arrays.hashCode(repeat)

    constructor(source: Parcel) : this(
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readString(),
            source.readString(),
            source.createIntArray(),
            source.readString(),
            source.createLongArray(),
            source.readLong(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readValue(Int::class.java.classLoader) as Int?
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
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AlarmItem> = object : Parcelable.Creator<AlarmItem> {
            override fun createFromParcel(source: Parcel): AlarmItem = AlarmItem(source)
            override fun newArray(size: Int): Array<AlarmItem?> = arrayOfNulls(size)
        }
    }
}