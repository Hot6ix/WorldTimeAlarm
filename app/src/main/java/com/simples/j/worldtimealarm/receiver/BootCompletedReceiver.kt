package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    private lateinit var db: AppDatabase
    private lateinit var activatedAlarms: ArrayList<AlarmItem>
    private lateinit var alarmController: AlarmController

    override fun onReceive(context: Context, intent: Intent) {
        // When device is rebooted, all alarm are cancelled automatically, so should re-schedule alarms.
        if(intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(C.TAG, Intent.ACTION_BOOT_COMPLETED)

            db = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseManager.DB_NAME)
                    .addMigrations(AppDatabase.MIGRATION_7_8)
                    .build()

            GlobalScope.launch(Dispatchers.IO) {
                alarmController = AlarmController.getInstance()
                activatedAlarms = ArrayList(db.alarmItemDao().getActivated())
                activatedAlarms.forEach {
                    alarmController.scheduleLocalAlarm(context, it, AlarmController.TYPE_ALARM)
                }
            }
        }
    }
}
