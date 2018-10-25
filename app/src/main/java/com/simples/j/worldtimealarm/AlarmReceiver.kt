package com.simples.j.worldtimealarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by j on 21/02/2018.
 *
 */
class AlarmReceiver(): BroadcastReceiver() {

    private lateinit var dbCursor: DatabaseCursor

    override fun onReceive(context: Context, intent: Intent) {
        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        val item = option.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
//        Log.d(C.TAG, "Alarm triggered : Info(${option.getParcelable<AlarmItem>(ITEM)})")
        Log.d(C.TAG, "Alarm triggered : ID(${item.notiId+1})")

        // Only one alarm shows to user, even if several alarm triggered at same time.
        // other alarms that lost in competition will be notified as missed.

        // If alarm alerted to user and until dismiss or snooze, also upcoming alarms will be notified as missed.
        if(!WakeUpActivity.isActivityRunning) {
            val wakeUpIntent = Intent(context, WakeUpActivity::class.java)
            wakeUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            wakeUpIntent.action = intent.action
            wakeUpIntent.putExtra(OPTIONS, option)
            context.startActivity(wakeUpIntent)
        }
        else {
//            Log.d(C.TAG, "Missed alarm alert : Info($item)")
            Log.d(C.TAG, "Alarm missed : ID(${item.notiId+1})")

            val mainIntent = Intent(context, MainActivity::class.java)
            val notification = NotificationCompat.Builder(context, context.packageName)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_action_alarm_white)
                    .setContentTitle(context.resources.getString(R.string.missed_alarm))
                    .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                    .setContentIntent(PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))

            if(item.label != null && item.label!!.isNotEmpty()) {
                notification.setStyle(NotificationCompat.BigTextStyle().bigText("${SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))} - ${item.label}"))
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(item.notiId, notification.build())
        }

        // If alarm type is snooze ignore, if not set alarm
        if(intent.action == AlarmReceiver.ACTION_ALARM) {
            dbCursor = DatabaseCursor(context)
            val repeatValue = context.resources.getIntArray(R.array.day_of_week_values)
            val repeat = item.repeat.mapIndexed { index, i -> if(i == 1) repeatValue[index] else 0 }.filter { it != 0 }

            if(repeat.isEmpty()) {
                // Disable one time alarm
                item.on_off = 0
                dbCursor.updateAlarm(item)
            }
            else {
                // If alarm is repeating, set next alarm
//                val calendar = Calendar.getInstance().apply {
//                    time = Date(item.timeSet.toLong())
//                }
//                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
//                val currentDayIndex = repeat.indexOf(currentDay)
//
//                if(currentDayIndex == repeat.lastIndex) {
//                    calendar.add(Calendar.WEEK_OF_MONTH, 1)
//                    calendar.set(Calendar.DAY_OF_WEEK, repeat[0])
//                }
//                else calendar.set(Calendar.DAY_OF_WEEK, repeat[currentDayIndex + 1])
//
//                item.timeSet = calendar.timeInMillis.toString()
//                dbCursor.updateAlarm(item)

                AlarmController.getInstance(context).scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
            }

            val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE)
            val bundle = Bundle()
            bundle.putParcelable(AlarmReceiver.ITEM, item)
            requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle)
            context.sendBroadcast(requestIntent)
        }
    }

    companion object {
        const val WAKE_LONG = 10000
        const val OPTIONS = "OPTIONS"
        const val ITEM = "ITEM"
        const val ACTION_SNOOZE = "com.simples.j.worldtimealarm.ACTION_SNOOZE"
        const val ACTION_ALARM = "com.simples.j.worldtimealarm.ACTION_ALARM"
    }

}