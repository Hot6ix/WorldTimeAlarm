package com.simples.j.worldtimealarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.AlarmWarningReason
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.WakeUpService
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.hasExtra(NOTIFICATION_ACTION)) {
            val action = intent.getStringExtra(NOTIFICATION_ACTION)
            Log.d(C.TAG, "Notification Action received: $action")

            when(action) {
                ACTION_SNOOZE -> {
                    intent.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)?.let {
                        AlarmController.getInstance().scheduleLocalAlarm(context, it, AlarmController.TYPE_SNOOZE)

                        val minutes = context.getString(R.string.minutes, it.snooze.div((60 * 1000)))
                        Toast.makeText(context, context.getString(R.string.alarm_on, minutes), Toast.LENGTH_SHORT).show()
                    }
                    val serviceActionIntent = Intent(WakeUpService.REQUEST_SERVICE_ACTION).apply { putExtra(WakeUpService.SERVICE_ACTION, AlarmReceiver.ACTION_SNOOZE) }
                    context.sendBroadcast(serviceActionIntent)
                    context.sendBroadcast(Intent(WakeUpActivity.ACTION_ACTIVITY_FINISH))
                }
                ACTION_DISMISS -> {
                    context.stopService(Intent(context, WakeUpService::class.java))
                    context.sendBroadcast(Intent(WakeUpActivity.ACTION_ACTIVITY_FINISH))
                }
                ACTION_APPLY_V22_UPDATE -> {
                    val alarmController = AlarmController()
                    val preference = PreferenceManager.getDefaultSharedPreferences(context)
                    val dbCursor = DatabaseCursor(context)

                    dbCursor.getActivatedAlarms().filter {
                        val applyDayRepeat = preference.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)
                        val oldResult = alarmController.calculateDate(it, TYPE_ALARM, applyDayRepeat)
                        val newResult = alarmController.calculateDateTime(it, TYPE_ALARM).toInstant().toEpochMilli()

                        val old = ZonedDateTime.ofInstant(Instant.ofEpochMilli(oldResult.timeInMillis), ZoneId.systemDefault())
                                .withNano(0)
                        val new = ZonedDateTime.ofInstant(Instant.ofEpochMilli(newResult), ZoneId.systemDefault())

                        !old.isEqual(new)
                    }.forEach {
                        alarmController.cancelAlarm(context, it.notiId)
                        val newResult = alarmController.scheduleLocalAlarm(context, it, TYPE_ALARM)

                        dbCursor.updateAlarm(it.apply { timeSet = newResult.toString() })
                    }

                    val requestIntent = Intent(MainActivity.ACTION_UPDATE_ALL).apply {
                        val bundle = Bundle().apply {
                            putString(AlarmItem.WARNING, "")
                            putString(AlarmItem.REASON, "")
                        }
                        putExtra(MultiBroadcastReceiver.BUNDLE, bundle)
                    }
                    context.sendBroadcast(requestIntent)

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(BuildConfig.VERSION_CODE)
                }
            }

        }
    }

    companion object {
        const val NOTIFICATION_ACTION = "NOTIFICATION_ACTION"
        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_APPLY_V22_UPDATE = "ACTION_APPLY_V22_UPDATE"
    }
}
