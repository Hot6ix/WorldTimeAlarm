package com.simples.j.worldtimealarm

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
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.GROUP_MISSED
import com.simples.j.worldtimealarm.etc.C.Companion.MISSED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by j on 21/02/2018.
 *
 */
class AlarmReceiver: BroadcastReceiver() {

    private lateinit var db: AppDatabase
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLocker: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager

    private var option: Bundle? = null
    private var isExpired = false

    override fun onReceive(context: Context, intent: Intent) {
        db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
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

        val itemFromIntent = option?.getParcelable<AlarmItem>(ITEM)

        GlobalScope.launch(Dispatchers.IO) {
            // get item from database using notification id
            val item = db.alarmItemDao().getAlarmItemFromNotificationId(itemFromIntent?.notiId)

            // nothing will happen if item is null
            if(item == null) {
                Log.d(C.TAG, "AlarmReceiver failed to get AlarmItem.")
                return@launch
            }
            Log.d(C.TAG, "Alarm(id=${item.notiId+1}, type=${intent.action}) triggered")

            // old alarm doesn't have id and index
            // replace old alarm item to alarm item from database that has id and index
            // id is necessary for highlighting item
            option = option?.apply {
                remove(ITEM)
                putParcelable(ITEM, item)
            }

            // check if alarm is expired
            isExpired = item.isExpired()
            // re-schedule alarm if it is repeating alarm and still valid
            if(!item.isInstantAlarm() && !isExpired) {
                AlarmController.getInstance().scheduleLocalAlarm(context, item, AlarmController.TYPE_ALARM)
            }

            // Only a single alarm will be shown to user, even if several alarms triggered at same time.
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
                Log.d(C.TAG, "Alarm(notiId=${item.notiId+1}, type=${intent.action}) missed")
                showMissedNotification(context, item)
                if(item.isInstantAlarm() || isExpired)
                    AlarmController.getInstance().disableAlarm(context, item)
            }
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(MISSED_NOTIFICATION_CHANNEL, context.getString(R.string.missed_notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

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