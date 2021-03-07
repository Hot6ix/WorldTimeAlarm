package com.simples.j.worldtimealarm.etc

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseManager
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by j on 26/02/2018.
 *
 */
@Entity(tableName = DatabaseManager.TABLE_ALARM_LIST)
data class AlarmItem(
        @PrimaryKey var id: Int?,
        @ColumnInfo(name = DatabaseManager.COLUMN_TIME_ZONE) var timeZone: String,
        @ColumnInfo(name = DatabaseManager.COLUMN_TIME_SET) var timeSet: String,
        @ColumnInfo(name = DatabaseManager.COLUMN_REPEAT) var repeat: IntArray,
        @ColumnInfo(name = DatabaseManager.COLUMN_RINGTONE) var ringtone: String?,
        @ColumnInfo(name = DatabaseManager.COLUMN_VIBRATION) var vibration: LongArray?,
        @ColumnInfo(name = DatabaseManager.COLUMN_SNOOZE) var snooze: Long,
        @ColumnInfo(name = DatabaseManager.COLUMN_LABEL) var label: String? = null,
        @ColumnInfo(name = DatabaseManager.COLUMN_ON_OFF) var on_off: Int,
        @ColumnInfo(name = DatabaseManager.COLUMN_NOTI_ID) var notiId: Int,
        @ColumnInfo(name = DatabaseManager.COLUMN_COLOR_TAG) var colorTag: Int,
        @ColumnInfo(name = DatabaseManager.COLUMN_INDEX) var index: Int?,
        @ColumnInfo(name = DatabaseManager.COLUMN_START_DATE) var startDate: Long? = null,
        @ColumnInfo(name = DatabaseManager.COLUMN_END_DATE) var endDate: Long? = null,
        @ColumnInfo(name = DatabaseManager.COLUMN_PICKER_TIME) var pickerTime: Long
) : Parcelable {

    override fun toString(): String {
        return "id=$id, timeZone=$timeZone, timeSet=$timeSet, repeat=${repeat.contentToString()}, ringtone=$ringtone, vibration=$vibration, snooze=$snooze, label=$label, on_off=$on_off, notiId=$notiId, colorTag=$colorTag, index=$index, startDate=$startDate, endDate=$endDate, picker_time=$pickerTime"
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
            source.readValue(Long::class.java.classLoader) as Long?,
            source.readLong()
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
        writeLong(pickerTime)
    }

    fun isInstantAlarm(): Boolean = repeat.all { it == 0 } && (endDate == null || endDate == 0L)

    fun hasRepeatDay(): Boolean = repeat.any { it > 0 }

    fun isExpired(dateTime: ZonedDateTime? = null): Boolean {
        if(startDate == null && endDate == null) return false

        val s =
                startDate.let {
                    if(it != null && it > 0) {
                        val startInstant = Instant.ofEpochMilli(it)
                        ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault())
                    }
                    else null
                }
        val e =
                endDate.let {
                    if(it != null && it > 0) {
                        val endInstant = Instant.ofEpochMilli(it)
                        ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault())
                    }
                    else null
                }

        var isExpired = false

        if(s != null && e != null) {
            when {
                s.isAfter(e) -> return true
                e.isBefore(ZonedDateTime.now()) || e.isEqual(ZonedDateTime.now()) -> return true
            }
        }

        s?.let {
            isExpired =
                if(!hasRepeatDay()) !s.isAfter(ZonedDateTime.now())
                else false
        }

        e?.let {
            if(e.isBefore(ZonedDateTime.now())) return true

            val next = dateTime ?: AlarmController().calculateDateTime(this, AlarmController.TYPE_ALARM)

            val repeatValues = intArrayOf(7, 1, 2, 3, 4, 5, 6)

            val isLastAlarmEndDate = repeat.mapIndexed { index, i ->
                if (i > 0) DayOfWeek.of(repeatValues[index])
                else null
            }.contains(e.dayOfWeek)

            isExpired = next.isAfter(e) || next.isBefore(ZonedDateTime.now()) || (next.isEqual(e.withSecond(0).withNano(0)) && !isLastAlarmEndDate && next.isBefore(ZonedDateTime.now()))
        }

        return isExpired
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AlarmItem> = object : Parcelable.Creator<AlarmItem> {
            override fun createFromParcel(source: Parcel): AlarmItem = AlarmItem(source)
            override fun newArray(size: Int): Array<AlarmItem?> = arrayOfNulls(size)
        }

        const val ALARM_ITEM_STATUS = "ALARM_ITEM_STATUS"
        const val WARNING = "WARNING"
        const val REASON = "REASON"
    }
}