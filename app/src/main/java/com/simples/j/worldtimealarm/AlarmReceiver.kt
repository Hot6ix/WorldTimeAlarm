package com.simples.j.worldtimealarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.GROUP_MISSED
import com.simples.j.worldtimealarm.etc.C.Companion.MISSED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.utils.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Created by j on 21/02/2018.
 *
 */
class AlarmReceiver: BroadcastReceiver(), CoroutineScope {

    private lateinit var db: AppDatabase
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLocker: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager
    private lateinit var preference: SharedPreferences

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private var option: Bundle? = null
    private var isExpired = false
    private var in24Hour = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineExceptionHandler

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()

        crashlytics.recordException(throwable)
    }

    override fun onReceive(context: Context, intent: Intent) {
        db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        preference = PreferenceManager.getDefaultSharedPreferences(context)

        in24Hour = preference.getBoolean(context.getString(R.string.setting_24_hr_clock_key), false)

        // Wake up screen
        wakeLocker = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, C.WAKE_TAG)
        wakeLocker.acquire(WAKE_LONG)

        option = intent.getBundleExtra(OPTIONS)
        if(option == null) {
            Log.d(C.TAG, "AlarmReceiver failed to get bundle.")
            return
        }

        val itemFromIntent = option?.getParcelable<AlarmItem>(ITEM)

        launch(Dispatchers.IO + coroutineExceptionHandler) {
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
                val scheduledTime = AlarmController.getInstance().scheduleLocalAlarm(context, item, AlarmController.TYPE_ALARM)
                if(scheduledTime == -1L) {
                    notificationManager.notify(
                        C.SHARED_NOTIFICATION_ID,
                        ExtensionHelper.getSimpleNotification(
                            context,
                            context.getString(R.string.scheduling_error_title),
                            context.getString(R.string.scheduling_error_message)
                        )
                    )

                    itemFromIntent?.let {
                        val offItem = it.apply {
                            on_off = 0
                        }

                        db.alarmItemDao().update(offItem)
                        val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE).apply {
                            val bundle = Bundle().apply {
                                putParcelable(ITEM, offItem)
                            }
                            putExtra(OPTIONS, bundle)
                        }
                        context.sendBroadcast(requestIntent)
                    }
                }
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

                // TODO: Find out how to show an alert when app failed to launch WakeUpService
                val permissionResult =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE)
                    else
                        null

                try {
                    if(Build.VERSION.SDK_INT >= 26)
                        context.startForegroundService(serviceIntent)
                    else
                        context.startService(serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()

                    WakeUpService.isWakeUpServiceRunning = false

                    itemFromIntent?.let {
                        val offItem = it.apply {
                            on_off = 0
                        }

                        AlarmController.getInstance().disableAlarm(context, offItem)
                        val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE).apply {
                            val bundle = Bundle().apply {
                                putParcelable(ITEM, offItem)
                            }
                            putExtra(OPTIONS, bundle)
                        }
                        context.sendBroadcast(requestIntent)
                    }

                    if(permissionResult == PackageManager.PERMISSION_DENIED) {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }

                        val pendingIntentFlag: Int =
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            else PendingIntent.FLAG_UPDATE_CURRENT

                        notificationManager.notify(
                            C.SHARED_NOTIFICATION_ID + 1,
                            ExtensionHelper.getSimpleNotification(
                                context,
                                context.getString(R.string.wakeup_permission_error_title),
                                context.getString(R.string.wakeup_permission_error_message),
                                PendingIntent.getActivity(context, 0, i, pendingIntentFlag)
                            )
                        )
                    }

                    coroutineExceptionHandler.handleException(coroutineContext, e)
                }
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

        val pendingIntentFlag: Int =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT

        notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentText(DateFormat.format(MediaCursor.getLocalizedTimeFormat(in24Hour), Date(item.timeSet.toLong())))
                .setContentIntent(PendingIntent.getActivity(context, item.notiId, dstIntent, pendingIntentFlag))
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
//                    .setContentText(context.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(context, item.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME)))
                    .setContentText(context.getString(R.string.alarm_no_long_fires).format(DateFormat.format(MediaCursor.getLocalizedTimeFormat(in24Hour), Date(item.timeSet.toLong()))))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .priority = NotificationCompat.PRIORITY_DEFAULT
        }

        if(item.label != null && !item.label.isNullOrEmpty()) {
//            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("${SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))} - ${item.label}"))
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("${DateFormat.format(MediaCursor.getLocalizedTimeFormat(in24Hour), Date(item.timeSet.toLong()))} - ${item.label}"))
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