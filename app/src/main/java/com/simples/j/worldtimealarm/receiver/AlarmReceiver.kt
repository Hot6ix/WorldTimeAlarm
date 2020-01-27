package com.simples.j.worldtimealarm.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import android.text.format.DateUtils
import android.util.Log
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.GROUP_MISSED
import com.simples.j.worldtimealarm.etc.C.Companion.MISSED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.WakeUpService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by j on 21/02/2018.
 *
 */
class AlarmReceiver: BroadcastReceiver() {

    private lateinit var dbCursor: DatabaseCursor
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLocker: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager

    private var option: Bundle? = null
    private var isExpired = false

    override fun onReceive(context: Context, intent: Intent) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Wake up screen
        wakeLocker = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, C.WAKE_TAG)
        wakeLocker.acquire(WAKE_LONG)

        option = intent.getBundleExtra(OPTIONS)
        if(option == null) {
            Log.d(C.TAG, "AlarmReceiver failed to get bundle.")
            return
        }

        val item = option?.getParcelable<AlarmItem>(ITEM)
        if(item == null) {
            Log.d(C.TAG, "AlarmReceiver failed to get AlarmItem.")
            return
        }
        Log.d(C.TAG, "Alarm(id=${item.notiId+1}, type=${intent.action}) triggered")

        // If alarm type is snooze ignore, if not set alarm
        dbCursor = DatabaseCursor(context)

        if(!item.isInstantAlarm()) {
            with(item.endDate) {
                try {
                    val endTimeInMillis = this
                    if(endTimeInMillis != null && endTimeInMillis > 0) {
                        val endDate = Calendar.getInstance().apply {
                            timeInMillis = endTimeInMillis
                        }

                        val today = Calendar.getInstance()
                        val difference = endDate.timeInMillis - today.timeInMillis
                        if(TimeUnit.MILLISECONDS.toDays(difference) < 7) {
                            val tmpCal = today.clone() as Calendar
                            while(!tmpCal.after(endDate)) {
                                tmpCal.add(Calendar.DATE, 1)
                                if(item.repeat.contains(tmpCal.get(Calendar.DAY_OF_WEEK))) {
                                    isExpired = false
                                    break
                                }
                                else isExpired = true
                            }

                            if((today.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) && today.get(Calendar.MONTH) == endDate.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) == endDate.get(Calendar.DAY_OF_MONTH)) || today.after(endDate))
                                isExpired = true
                        }
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }

            if(!isExpired) {
                AlarmController.getInstance().scheduleAlarm(context, item, AlarmController.TYPE_ALARM)
            }
        }

        // Only a single alarm will be shown to user, even if several alarm triggered at same time.
        // others will be notified as missed.

        // If alarm alerted to user and until dismiss or snooze, also upcoming alarms will be notified as missed.
        if(!WakeUpService.isWakeUpServiceRunning) {
            WakeUpService.isWakeUpServiceRunning = true

            val serviceIntent = Intent(context, WakeUpService::class.java).apply {
                putExtra(OPTIONS, option)
                putExtra(EXPIRED, isExpired)
                action = intent.action
            }

            if(Build.VERSION.SDK_INT >= 26)
                context.startForegroundService(serviceIntent)
            else
                context.startService(serviceIntent)
        }
        else {
            Log.d(C.TAG, "Alarm(id=${item.notiId+1}, type=${intent.action}) missed")
            showMissedNotification(context, item)
            if(item.isInstantAlarm() || isExpired)
                AlarmController.getInstance().disableAlarm(context, item)
        }
    }

    private fun showMissedNotification(context: Context, item: AlarmItem) {
        val notificationBuilder = NotificationCompat.Builder(context, MISSED_NOTIFICATION_CHANNEL)

        val dstIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(AlarmListFragment.HIGHLIGHT_KEY, item.notiId)
        }

        val title = context.resources.getString(R.string.missed_alarm)

        notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                .setContentIntent(PendingIntent.getActivity(context, item.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setGroup(GROUP_MISSED)

        if(isExpired) {
            notificationBuilder
                    .setContentTitle(context.resources.getString(R.string.missed_and_last_alarm))
                    .setContentText(context.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(context, item.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME)))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .priority = NotificationCompat.PRIORITY_DEFAULT
        }

        if(item.label != null && !item.label.isNullOrEmpty()) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("${SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))} - ${item.label}"))
        }

        notificationManager.notify(item.notiId, notificationBuilder.build())
    }

    companion object {
        const val WAKE_LONG = 10000L
        const val OPTIONS = "OPTIONS"
        const val EXPIRED = "EXPIRED"
        const val ITEM = "ITEM"
        const val ACTION_SNOOZE = "com.simples.j.worldtimealarm.ACTION_SNOOZE"
        const val ACTION_ALARM = "com.simples.j.worldtimealarm.ACTION_ALARM"

        const val TYPE_ALARM = 0
        const val TYPE_EXPIRED = 1
    }

}