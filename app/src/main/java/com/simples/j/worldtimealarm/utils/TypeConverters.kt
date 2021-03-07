package com.simples.j.worldtimealarm.utils

import androidx.room.TypeConverter

class TypeConverters {
    @TypeConverter
    fun repeatToString(repeat: IntArray): String {
        return repeat.contentToString()
    }

    @TypeConverter
    fun stringToRepeat(repeat: String): IntArray {
        return repeat.replace("[", "").replace("]", "").split(",").map { it.trim().toInt() }.toIntArray()
    }

    @TypeConverter
    fun vibrationToString(vibration: LongArray?): String {
        return vibration.contentToString()
    }

    @TypeConverter
    fun stringToVibration(vibration: String?): LongArray? {
        return if(vibration.isNullOrEmpty() || vibration == "null") null
        else vibration.replace("[", "").replace("]", "").split(",").map { it.trim().toLong() }.toLongArray()
    }
}