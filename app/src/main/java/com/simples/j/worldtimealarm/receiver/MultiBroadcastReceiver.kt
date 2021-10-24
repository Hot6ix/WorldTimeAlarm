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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.simples.j.worldtimealarm.BuildConfig
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem.Companion.REASON
import com.simples.j.worldtimealarm.etc.AlarmItem.Companion.WARNING
import com.simples.j.worldtimealarm.etc.AlarmWarningReason
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.DstController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class MultiBroadcastReceiver : BroadcastReceiver() {

    private lateinit var db: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(C.TAG, "${intent.action}")

        val alarmController = AlarmController()
        val dstController = DstController(context)
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_4_8, AppDatabase.MIGRATION_5_8, AppDatabase.MIGRATION_7_8)
                .build()

        // set up notification
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(C.DEFAULT_NOTIFICATION_CHANNEL, context.getString(R.string.default_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val activated = db.alarmItemDao().getActivated()

            when(intent.action) {
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    when(BuildConfig.VERSION_CODE) {
                        22, 23 -> {
                            activated.forEachIndexed { index, alarmItem ->
                                val updated = alarmItem.apply {
                                    this.index = index
                                    this.pickerTime = alarmItem.timeSet.toLong()
                                }

                                db.alarmItemDao().update(updated)
                            }

                            if(activated.isNotEmpty()) {
                                // re-schedule all activated alarms
                                activated.filter { it.on_off == 1 }.filter {
                                    val applyDayRepeat = preference.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)
                                    val oldResult =
                                            try {
                                                alarmController.calculateDate(it, AlarmController.TYPE_ALARM, applyDayRepeat)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                return@filter false
                                            }
                                    val newResult = alarmController.calculateDateTime(it, AlarmController.TYPE_ALARM).toInstant().toEpochMilli()

                                    val old = ZonedDateTime.ofInstant(Instant.ofEpochMilli(oldResult.timeInMillis), ZoneId.systemDefault())
                                            .withNano(0)
                                    val new = ZonedDateTime.ofInstant(Instant.ofEpochMilli(newResult), ZoneId.systemDefault())

                                    !old.isEqual(new)
                                }.also {
                                    if(it.isEmpty()) {
                                        val msg = context.getString(R.string.v22_time_set_message)
                                        val notification = getNotification(
                                                context,
                                                context.getString(R.string.v22_change_dialog_title),
                                                msg
                                        ).apply {
                                            setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                                            setAutoCancel(true)
                                        }.build()
                                        notificationManager.notify(BuildConfig.VERSION_CODE, notification)
                                    }
                                    else {
                                        val applyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                                            putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, NotificationActionReceiver.ACTION_APPLY_V22_UPDATE)
                                        }

                                        val pendingIntentFlag: Int =
                                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                            else PendingIntent.FLAG_UPDATE_CURRENT

                                        val applyPendingIntent = PendingIntent.getBroadcast(context, BuildConfig.VERSION_CODE, applyIntent, pendingIntentFlag)
                                        val applyAction = NotificationCompat.Action(0, context.getString(R.string.apply_changes), applyPendingIntent)

                                        val msg = context.getString(R.string.v22_time_set_message_change, it.size)
                                        val notification = getNotification(
                                                context,
                                                context.getString(R.string.confirm_before_change),
                                                msg,
                                                Bundle().apply {
                                                    putString(WARNING, it.map { item -> item.id }.joinToString(","))
                                                    putString(REASON, IntArray(it.size) { AlarmWarningReason.REASON_V22_UPDATE.reason }.joinToString(","))
                                                }
                                        ).apply {
                                            setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                                            setAutoCancel(false)
                                            addAction(applyAction)
                                        }.build()
                                        notificationManager.notify(BuildConfig.VERSION_CODE, notification)
                                    }
                                }
                            }

                            preference.edit()
                                    .putBoolean(context.getString(R.string.setting_converter_timezone_key), true)
                                    .apply()
                        }
                    }
                }
                DstController.ACTION_DST_CHANGED -> {
                    // Re-schedule all alarms
                    val targetMillis = intent.getLongExtra(AlarmController.EXTRA_TIME_IN_MILLIS, -1)
                    val dstList = db.dstItemDao().getAll()
                    val millisGroup = dstList.groupBy {
                        it.millis
                    }

                    millisGroup.forEach { currentGroup ->
                        if(currentGroup.key == targetMillis) {
                            currentGroup.value.groupBy { dstItem ->
                                val zoneId = ZoneId.of(dstItem.timeZone)
                                val targetZonedDateTime = zoneId.rules.nextTransition(Instant.now()).dateTimeAfter.atZone(zoneId)

                                targetZonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }.forEach { afterGroup ->
                                afterGroup.value.forEach {
                                    val updated = it.apply {
                                        millis = afterGroup.key
                                    }
                                    db.dstItemDao().update(updated)
                                }

                                if(!millisGroup.containsKey(afterGroup.key)) {
                                    dstController.scheduleNextDaylightSavingTimeAlarm(afterGroup.key)
                                }
                            }
                        }
                    }

                    context.sendBroadcast(Intent(MainActivity.ACTION_UPDATE_ALL))
                }
                else -> {

                }
            }
        }

    }

    private fun getNotification(context: Context, title: String, msg: String, extra: Bundle? = null): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(context, C.DEFAULT_NOTIFICATION_CHANNEL)

        val dstIntent = Intent(context, MainActivity::class.java).apply {
            extra?.let { putExtra(BUNDLE, it) }
            flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlag: Int =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT

        notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(PendingIntent.getActivity(context, BuildConfig.VERSION_CODE, dstIntent, pendingIntentFlag))
                .setGroup(C.GROUP_DEFAULT)
                .setDefaults(Notification.DEFAULT_ALL)
                .priority = NotificationCompat.PRIORITY_DEFAULT

        return notificationBuilder
    }

    companion object {
        const val BUNDLE = "BUNDLE"
    }
}
