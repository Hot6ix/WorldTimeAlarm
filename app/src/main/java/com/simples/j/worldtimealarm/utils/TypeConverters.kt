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

    @TypeConverter
    fun intPairArrayToString(pairArray: Array<Pair<Int, IntArray>>?): String {
        return pairArray?.joinToString(prefix = "[", postfix = "]", separator = ";") {
            "(${it.first}@${it.second.joinToString(prefix = "[", postfix = "]")})"
        } ?: ""
    }

    @TypeConverter
    fun stringToIntPairArray(intPairArray: String?):  Array<Pair<Int, IntArray>>?{
        return if(intPairArray.isNullOrEmpty()) null
        else {
            val first = intPairArray
                    .removeSurrounding("[", "]")
                    .split(";")
            val list = ArrayList<Pair<Int, IntArray>>()
            first.forEach { t ->
                val ordinal = t
                        .removeSurrounding("(", ")")
                        .split("@")
                list.add(
                        Pair(
                                ordinal[0].toInt(),
                                ordinal[1].removeSurrounding("[", "]").split(",").toTypedArray().map { it.trim().toInt() }.toIntArray()
                        )
                )
            }

            return list.toTypedArray()
        }

    }
}