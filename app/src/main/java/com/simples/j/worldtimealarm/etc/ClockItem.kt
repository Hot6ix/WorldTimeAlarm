package com.simples.j.worldtimealarm.etc

import android.os.Parcel
import android.os.Parcelable

data class ClockItem(var id: Int?, var timezone: String?, var index: Int?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readString(),
            source.readValue(Int::class.java.classLoader) as Int?
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeString(timezone)
        writeValue(index)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ClockItem> = object : Parcelable.Creator<ClockItem> {
            override fun createFromParcel(source: Parcel): ClockItem = ClockItem(source)
            override fun newArray(size: Int): Array<ClockItem?> = arrayOfNulls(size)
        }
    }
}