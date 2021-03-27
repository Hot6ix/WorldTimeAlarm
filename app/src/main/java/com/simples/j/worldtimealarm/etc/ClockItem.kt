package com.simples.j.worldtimealarm.etc

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simples.j.worldtimealarm.utils.DatabaseManager

@Entity(tableName = DatabaseManager.TABLE_CLOCK_LIST)
data class ClockItem(
        @ColumnInfo(name = DatabaseManager.COLUMN_ID) var id: Int?,
        @PrimaryKey @ColumnInfo(name = DatabaseManager.COLUMN_TIME_ZONE) var timezone: String,
        @ColumnInfo(name = DatabaseManager.COLUMN_INDEX) var index: Int?
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readString().toString(),
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