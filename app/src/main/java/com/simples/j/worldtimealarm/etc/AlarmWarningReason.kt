package com.simples.j.worldtimealarm.etc

enum class AlarmWarningReason(val reason: Int) {
    REASON_V22_UPDATE(22),
    REASON_UNKNOWN(-1);

    companion object {
        fun valueOf(reason: Int?): AlarmWarningReason {
            return values().find { it.reason == reason } ?: REASON_UNKNOWN
        }
    }
}