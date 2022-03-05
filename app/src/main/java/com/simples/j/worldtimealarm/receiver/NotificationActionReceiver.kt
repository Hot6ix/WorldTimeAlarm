package com.simples.j.worldtimealarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.*
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class NotificationActionReceiver : BroadcastReceiver() {
    // TODO:  Check RuntimeException/SecurityException
    private lateinit var db: AppDatabase
    private lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.hasExtra(NOTIFICATION_ACTION)) {
            val action = intent.getStringExtra(NOTIFICATION_ACTION)
            Log.d(C.TAG, "Notification Action received: $action")

            db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                    .build()
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            when(action) {
                ACTION_SNOOZE -> {
                    intent.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)?.let {
                        val scheduledTime = AlarmController.getInstance().scheduleLocalAlarm(context, it, AlarmController.TYPE_SNOOZE)

                        if(scheduledTime == -1L) {
                            notificationManager.notify(
                                C.SHARED_NOTIFICATION_ID,
                                ExtensionHelper.getSimpleNotification(
                                    context,
                                    context.getString(R.string.snooze_scheduling_error_title),
                                    context.getString(R.string.snooze_scheduling_error_message)
                                )
                            )

//                            it.let {
//                                val offItem = it.apply {
//                                    on_off = 0
//                                }
//
//                                AlarmController.getInstance().disableAlarm(context, offItem)
//                                val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE).apply {
//                                    val bundle = Bundle().apply {
//                                        putParcelable(AlarmReceiver.ITEM, offItem)
//                                    }
//                                    putExtra(AlarmReceiver.OPTIONS, bundle)
//                                }
//                                context.sendBroadcast(requestIntent)
//                            }
                        }
                        else {
                            val minutes = context.getString(R.string.minutes, it.snooze.div((60 * 1000)))
                            Toast.makeText(context, context.getString(R.string.alarm_on, minutes), Toast.LENGTH_SHORT).show()
                        }
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

                    CoroutineScope(Dispatchers.IO).launch {
                        db.alarmItemDao().getActivated().filter {
                            val applyDayRepeat = preference.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)
                            val oldResult =
                                    try {
                                        alarmController.calculateDate(it, TYPE_ALARM, applyDayRepeat)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        return@filter false
                                    }
                            val newResult = alarmController.calculateDateTime(it, TYPE_ALARM).toInstant().toEpochMilli()

                            val old = ZonedDateTime.ofInstant(Instant.ofEpochMilli(oldResult.timeInMillis), ZoneId.systemDefault())
                                    .withNano(0)
                            val new = ZonedDateTime.ofInstant(Instant.ofEpochMilli(newResult), ZoneId.systemDefault())

                            !old.isEqual(new)
                        }.forEach {
                            alarmController.cancelAlarm(context, it.notiId)
                            val newResult = alarmController.scheduleLocalAlarm(context, it, TYPE_ALARM)

                            db.alarmItemDao().update(it.apply { timeSet = newResult.toString() })
                        }
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
