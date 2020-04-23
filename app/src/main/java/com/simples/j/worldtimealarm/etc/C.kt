package com.simples.j.worldtimealarm.etc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.ads.RequestConfiguration
import com.simples.j.worldtimealarm.AlarmReceiver
import com.simples.j.worldtimealarm.WakeUpActivity
import org.threeten.bp.Instant
import java.util.*

/**
 * Created by j on 05/03/2018.
 */
class C {
    companion object {
        const val SHARED_NOTIFICATION_ID = 12848427
        const val SERVICE_ID = 12848427
        const val WAKE_TAG = "worldtime:alarm"
        const val TAG = "tagggggg"

        const val ALARM_NOTIFICATION_CHANNEL = "ALARM_NOTIFICATION_CHANNEL"
        const val MISSED_NOTIFICATION_CHANNEL = "MISSED_NOTIFICATION_CHANNEL"
        const val EXPIRED_NOTIFICATION_CHANNEL = "EXPIRED_NOTIFICATION_CHANNEL"
        const val DEFAULT_NOTIFICATION_CHANNEL = "DEFAULT_NOTIFICATION_CHANNEL"

        const val GROUP_MISSED = "com.simples.j.worldtimealarm.GROUP_MISSED"
        const val GROUP_EXPIRED = "com.simples.j.worldtimealarm.GROUP_EXPIRED"
        const val GROUP_DEFAULT = "com.simples.j.worldtimealarm.GROUP_DEFAULT"

        fun createAlarm(timeZone: String,
                                timeSet: Instant = Instant.now(),
                                repeat: IntArray = IntArray(7) {0},
                                snooze: Long = 0L,
                                label: String? = null,
                                startDate: Long? = null,
                                endDate: Long? = null): AlarmItem {
            val notiId = 100000 + Random().nextInt(899999)
            return AlarmItem(
                    null,
                    timeZone,
                    timeSet.toEpochMilli().toString(),
                    repeat,
                    null,
                    null,
                    snooze,
                    label,
                    1,
                    notiId,
                    0,
                    0,
                    startDate,
                    endDate,
                    timeSet.toEpochMilli()
            )
        }

        fun createWakeUpIntent(context: Context, item: AlarmItem): Intent {
            val bundle = Bundle().apply {
                putParcelable(AlarmReceiver.ITEM, item)
            }

            return Intent(context, WakeUpActivity::class.java).apply {
                putExtra(AlarmReceiver.OPTIONS, bundle)
                putExtra(AlarmReceiver.EXPIRED, false)
                action = AlarmReceiver.ACTION_ALARM
            }
        }

        fun getAdsTestConfig(): RequestConfiguration {
            val testDevices = arrayListOf("5E85E343F2722B2AE300110EE20B92D8")
            val reqConfig = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDevices)

            return reqConfig.build()
        }
    }
}