package com.simples.j.worldtimealarm.etc

import java.io.Serializable

data class SnoozeItem(var title: String, var duration: Long): Serializable {
    override fun toString(): String {
        return "$title, $duration"
    }
}