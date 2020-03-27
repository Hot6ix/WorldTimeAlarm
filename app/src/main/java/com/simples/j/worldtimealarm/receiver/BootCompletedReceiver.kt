package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor

class BootCompletedReceiver : BroadcastReceiver() {

    private lateinit var dbCursor: DatabaseCursor
    private lateinit var activatedAlarms: ArrayList<AlarmItem>
    private lateinit var alarmController: AlarmController

    override fun onReceive(context: Context, intent: Intent) {
        // When device rebooted, all alarm had been cancelled automatically, so should re-schedule alarms.
        if(intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(C.TAG, Intent.ACTION_BOOT_COMPLETED)
            dbCursor = DatabaseCursor(context)
            alarmController = AlarmController.getInstance()
            activatedAlarms = dbCursor.getActivatedAlarms()
            activatedAlarms.forEach {
                alarmController.scheduleLocalAlarm(context, it, AlarmController.TYPE_ALARM)
            }
        }
    }
}
