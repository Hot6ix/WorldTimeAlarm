package com.simples.j.worldtimealarm.etc

import java.io.Serializable
import java.util.*

/**
 * Created by j on 20/02/2018.
 *
 */
data class PatternItem(var title: String, var array: LongArray?): Serializable {
    override fun toString(): String {
        return "$title, ${Arrays.toString(array)}"
    }

    override fun equals(other: Any?): Boolean {
        val item = other as PatternItem
        return Arrays.equals(array, item.array)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(array)
    }
}