package com.simples.j.worldtimealarm.receiver

import android.app.Notification
import android.app.NotificationChannel
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
        Log.d(C.TAG, "Alarm triggered : ID(${item.notiId+1})")

        // If alarm type is snooze ignore, if not set alarm
        if(intent.action == ACTION_ALARM) {
            dbCursor = DatabaseCursor(context)

            val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE)
            val bundle = Bundle().apply {
                putParcelable(ITEM, item)
            }
            requestIntent.putExtra(OPTIONS, bundle)

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

                if(isExpired) {
                    // Alarm has been expired
                    Log.d(C.TAG, "Alarm expired : ID(${item.notiId+1})")
                    item.on_off = 0
                    dbCursor.updateAlarm(item)
                    context.sendBroadcast(requestIntent)
                }
                else
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
            }

            if(Build.VERSION.SDK_INT >= 26)
                context.startForegroundService(serviceIntent)
            else
                context.startService(serviceIntent)
//            WakeUpActivity.isActivityRunning = true
//            Intent(context, WakeUpActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                action = intent.action
//                putExtra(OPTIONS, option)
//                putExtra(EXPIRED, isExpired)
//                context.startActivity(this)
//            }
        }
        else {
            Log.d(C.TAG, "Alarm missed : ID(${item.notiId+1})")
            showMissedNotification(context, item)
        }
    }

    private fun showMissedNotification(context: Context, item: AlarmItem) {
        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        val dstIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(AlarmListFragment.HIGHLIGHT_KEY, item.notiId)
        }

        val title = context.resources.getString(R.string.missed_alarm)

        notificationBuilder
                .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                .setContentIntent(PendingIntent.getActivity(context, item.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT))

        if(isExpired) {
            notificationBuilder
                    .setAutoCancel(true)
                    .setContentTitle(context.resources.getString(R.string.missed_and_last_alarm))
                    .setContentText(context.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(context, item.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME)))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .priority = NotificationCompat.PRIORITY_MAX
        }

        if(item.label != null && !item.label.isNullOrEmpty()) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("${SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))} - ${item.label}"))
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)

            }
            notificationManager.createNotificationChannel(notificationChannel)
        }


        notificationBuilder
                .setVibrate(LongArray(0))
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)

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
        const val TYPE_MISSED = 2

        const val NOTIFICATION_CHANNEL_ID = "WorldTimeAlarmNotificationChannel"
    }

}