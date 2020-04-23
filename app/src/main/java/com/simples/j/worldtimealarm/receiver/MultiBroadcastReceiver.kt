package com.simples.j.worldtimealarm.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.simples.j.worldtimealarm.BuildConfig
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.DstController
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId

class MultiBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(C.TAG, "${intent.action}")
        val db = DatabaseCursor(context)
        val alarmList = db.getActivatedAlarms()
        val alarmController = AlarmController.getInstance()
        val dstController = DstController(context)

        when(intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                when(BuildConfig.VERSION_CODE) {
                    22 -> {
                        // reset order of list
                        alarmList.forEachIndexed { index, alarmItem ->
                            alarmItem.index = index
                            alarmItem.pickerTime = alarmItem.timeSet.toLong()

                            db.updateAlarmIndex(alarmItem)
                        }

                        alarmList.filter {
                            val applyTimeDiffToRepeat = PreferenceManager.getDefaultSharedPreferences(context)
                                    .getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)
                            val oldResult = alarmController.calculateDate(it, AlarmController.TYPE_ALARM, applyTimeDiffToRepeat)
                            val newResult = alarmController.calculateDateTime(it, AlarmController.TYPE_ALARM)

                            oldResult.timeInMillis != newResult.toInstant().toEpochMilli()
                        }.let { list ->
                            if(list.isNotEmpty()) {
                                showNotification(
                                        context,
                                        context.getString(R.string.version_code_22_update_title),
                                        context.getString(R.string.version_code_22_update_message),
                                        true)
                            }
                        }

                        val preference = PreferenceManager.getDefaultSharedPreferences(context)
                        preference.edit()
                                .putBoolean(context.getString(R.string.setting_converter_timezone_key), true)
                                .apply()
                    }
                }
            }
            DstController.ACTION_DST_CHANGED -> {
                // Re-schedule all alarms
                val targetMillis = intent.getLongExtra(AlarmController.EXTRA_TIME_IN_MILLIS, -1)
                val dstList = db.getDstList()
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
                                db.updateDst(updated)
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

    private fun showNotification(context: Context, title: String, msg: String, isLongMsg: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, C.DEFAULT_NOTIFICATION_CHANNEL)

        val dstIntent = Intent(context, MainActivity::class.java)

        notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(PendingIntent.getActivity(context, BuildConfig.VERSION_CODE, dstIntent, PendingIntent.FLAG_ONE_SHOT))
                .setGroup(C.GROUP_DEFAULT)
                .setDefaults(Notification.DEFAULT_ALL)
                .priority = NotificationCompat.PRIORITY_DEFAULT

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(C.DEFAULT_NOTIFICATION_CHANNEL, context.getString(R.string.default_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        if(isLongMsg) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(msg))
        }

        notificationManager.notify(BuildConfig.VERSION_CODE, notificationBuilder.build())
    }
}
