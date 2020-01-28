package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.WakeUpActivity
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.WakeUpService

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.hasExtra(NOTIFICATION_ACTION)) {
            val action = intent.getStringExtra(NOTIFICATION_ACTION)
            Log.d(C.TAG, "Notification Action received: $action")

            if (action == ACTION_SNOOZE) {
                val alarmItem = intent.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)
                AlarmController.getInstance().scheduleAlarm(context, alarmItem, AlarmController.TYPE_SNOOZE)

                val minutes = context.getString(R.string.minutes, alarmItem.snooze.div((60 * 1000)))
                Toast.makeText(context, context.getString(R.string.alarm_on, minutes), Toast.LENGTH_SHORT).show()
                val serviceActionIntent = Intent(WakeUpService.REQUEST_SERVICE_ACTION).apply { putExtra(WakeUpService.SERVICE_ACTION, AlarmReceiver.ACTION_SNOOZE) }
                context.sendBroadcast(serviceActionIntent)
            }
            else {
                context.stopService(Intent(context, WakeUpService::class.java))
            }

            context.sendBroadcast(Intent(WakeUpActivity.ACTION_ACTIVITY_FINISH))
        }
    }

    companion object {
        const val NOTIFICATION_ACTION = "NOTIFICATION_ACTION"
        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
    }
}
