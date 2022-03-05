package com.simples.j.worldtimealarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.ExtensionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    private lateinit var db: AppDatabase
    private lateinit var activatedAlarms: ArrayList<AlarmItem>
    private lateinit var alarmController: AlarmController
    private lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        // When device is rebooted, all alarm are cancelled automatically, so should re-schedule alarms.
        if(intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(C.TAG, Intent.ACTION_BOOT_COMPLETED)

            db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                    .build()
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            CoroutineScope(Dispatchers.IO).launch {
                alarmController = AlarmController.getInstance()
                activatedAlarms = ArrayList(db.alarmItemDao().getActivated())

                activatedAlarms.map {
                    val scheduledTime = alarmController.scheduleLocalAlarm(context, it, AlarmController.TYPE_ALARM)

                    Pair(it, scheduledTime)
                }.filter { it.second == -1L }.also {
                    if(it.isNotEmpty()) {
                        notificationManager.notify(
                            C.SHARED_NOTIFICATION_ID,
                            ExtensionHelper.getSimpleNotification(
                                context,
                                context.getString(R.string.scheduling_error_title),
                                context.getString(R.string.scheduling_error_message)
                            )
                        )
                    }
                }

            }
        }
    }
}
