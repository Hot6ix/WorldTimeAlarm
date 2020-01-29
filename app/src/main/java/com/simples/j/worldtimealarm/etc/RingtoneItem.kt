package com.simples.j.worldtimealarm.etc

import java.io.Serializable

/**
 * Created by j on 07/03/2018.
 *
 */

data class RingtoneItem(var title: String, var uri: String?): Serializable {
    override fun toString(): String {
        return "$title, $uri"
    }

    override fun equals(other: Any?): Boolean {
        val item = other as RingtoneItem
        return uri == item.uri
    }

    override fun hashCode(): Int {
        return uri?.hashCode() ?: -1
    }
}