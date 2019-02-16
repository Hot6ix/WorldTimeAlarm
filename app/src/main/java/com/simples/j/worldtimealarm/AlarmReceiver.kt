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
class AlarmReceiver: BroadcastReceiver() {

    private lateinit var dbCursor: DatabaseCursor

    override fun onReceive(context: Context, intent: Intent) {
        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        val item = option.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
        if(item == null) {
            Log.d(C.TAG, "AlarmReceiver failed to get AlarmItem")
            return
        }
        Log.d(C.TAG, "Alarm triggered : ID(${item.notiId+1})")

        // If alarm type is snooze ignore, if not set alarm
        var isExpired = false
        if(intent.action == AlarmReceiver.ACTION_ALARM) {
            dbCursor = DatabaseCursor(context)

            val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE)
            val bundle = Bundle().apply {
                putParcelable(AlarmReceiver.ITEM, item)
            }
            requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle)

            if(!item.repeat.any { it > 0 }) {
                // Disable one time alarm
                item.on_off = 0
                dbCursor.updateAlarm(item)
                context.sendBroadcast(requestIntent)
            }
            else {
                with(item.endDate) {
                    try {
                        val endTimeInMillis = this
                        if(endTimeInMillis != null && endTimeInMillis > 0) {
                            val endDate = Calendar.getInstance().apply {
                                timeInMillis = endTimeInMillis
                            }

                            val today = Calendar.getInstance()
                            isExpired = (today.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) && today.get(Calendar.MONTH) == endDate.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) == endDate.get(Calendar.DAY_OF_MONTH)) || today.after(endDate)
                        }
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }

                if(isExpired) {
                    // Alarm has been expired
                    Log.d(C.TAG, "Alarm expired : ID(${item.notiId+1})")
                    item.on_off = 0
                    dbCursor.updateAlarm(item)
                    context.sendBroadcast(requestIntent)
                }
                else
                    AlarmController.getInstance(context).scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
            }
        }

        // Only a single alarm are showing to user, even if several alarm triggered at same time.
        // other alarms that lost in competition will be notified as missed.

        // If alarm alerted to user and until dismiss or snooze, also upcoming alarms will be notified as missed.
        if(!WakeUpActivity.isActivityRunning) {
            Intent(context, WakeUpActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                action = intent.action
                putExtra(OPTIONS, option)
                putExtra(EXPIRED, isExpired)
                context.startActivity(this)
            }
        }
        else {
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
    }

    companion object {
        const val WAKE_LONG = 10000
        const val OPTIONS = "OPTIONS"
        const val EXPIRED = "EXPIRED"
        const val ITEM = "ITEM"
        const val ACTION_SNOOZE = "com.simples.j.worldtimealarm.ACTION_SNOOZE"
        const val ACTION_ALARM = "com.simples.j.worldtimealarm.ACTION_ALARM"
    }

}