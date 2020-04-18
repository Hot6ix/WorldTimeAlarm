package com.simples.j.worldtimealarm.utils

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.DstItem
import com.simples.j.worldtimealarm.receiver.MultiBroadcastReceiver
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

class DstController(private val context: Context) {

    private val dbCursor = DatabaseCursor(context)

    fun handleSystemDst() {
        val timeZone = TimeZone.getDefault()
        val zoneId = ZoneId.systemDefault()

        if(timeZone.useDaylightTime()) {
            val systemDst = dbCursor.getSystemDst()
            val nextDateTimeAfter = zoneId.rules.nextTransition(Instant.now()).dateTimeAfter.atZone(zoneId).toInstant()
            if(systemDst == null) {
                val id = dbCursor.insertDst(nextDateTimeAfter.toEpochMilli(), timeZone.id, -1)

                if(id > 0) {
                    checkAndScheduleDst(DstItem(id, nextDateTimeAfter.toEpochMilli(), zoneId.id, -1))
                    Log.d(C.TAG, "Schedule system dst alarm")
                }
            }
        }
        else {
            dbCursor.getSystemDst()?.let {
                cancelDaylightSavingTimeAlarm(it.millis)
                dbCursor.removeSystemDst()
                Log.d(C.TAG, "System time zone doesn't use dst")
            }
        }
    }

    fun checkAndScheduleDst(item: DstItem?) {
        if(item == null) return

        val list = dbCursor.getDstList()

        with(list.filter { it.millis == item.millis && it.alarmId != item.alarmId }) {
            if(size == 0 || this.all { dbCursor.getSingleAlarm(it.alarmId)?.on_off == 0 }) {
                scheduleNextDaylightSavingTimeAlarm(item.millis)
                Log.d(C.TAG, "Schedule dstItem($item) alarm for ${ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.millis), ZoneId.systemDefault())}.")
            }
            else {
                Log.d(C.TAG, "Same dst alarm already scheduled.")
            }
        }
    }

    fun requestDstCancellation(item: DstItem) {
        val list = dbCursor.getDstList()

        with(list.filter { it.millis == item.millis && it.alarmId != item.alarmId }) {
            if(size == 0 || this.all { dbCursor.getSingleAlarm(it.alarmId)?.on_off == 0 }) {
                cancelDaylightSavingTimeAlarm(item.millis)
                Log.d(C.TAG, "dstItem($item) alarm cancelled.")
            }
            else {
                Log.d(C.TAG, "dstItem($item) alarm failed to cancel due to other alarm.")
            }
        }
    }

    fun scheduleNextDaylightSavingTimeAlarm(millis: Long?): Long {
        if (millis == null || millis < 0) return -1L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MultiBroadcastReceiver::class.java).apply {
            action = ACTION_DST_CHANGED
            putExtra(AlarmController.EXTRA_TIME_IN_MILLIS, millis)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT < 23) {
            val mainIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.setAlarmClock(AlarmClockInfo(millis, mainIntent), pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }
        Log.d(C.TAG, "DST scheduled: ${ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())}")

        return millis
    }

    fun cancelDaylightSavingTimeAlarm(millis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MultiBroadcastReceiver::class.java).apply {
            action = ACTION_DST_CHANGED
            putExtra(AlarmController.EXTRA_TIME_IN_MILLIS, millis)
        }

        val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(alarmIntent)
        Log.d(C.TAG, "DST cancelled: ${ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())}")
    }

    companion object {
        const val ACTION_DST_CHANGED = "ACTION_DST_CHANGED"
    }
}