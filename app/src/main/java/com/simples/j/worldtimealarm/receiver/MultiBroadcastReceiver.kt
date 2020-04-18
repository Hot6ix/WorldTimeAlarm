package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
                        alarmList.forEachIndexed { index, alarmItem ->
                            alarmItem.index = index
                            alarmItem.pickerTime = alarmItem.timeSet.toLong()

                            db.updateAlarmIndex(alarmItem)

                            if(alarmItem.on_off == 1) {
                                alarmController.cancelAlarm(context, alarmItem.notiId)
                                alarmController.scheduleLocalAlarm(context, alarmItem, AlarmController.TYPE_ALARM)
                            }

                            val zoneId = ZoneId.of(alarmItem.timeZone)
                            db.insertDst(
                                    zoneId.rules.nextTransition(Instant.now()).dateTimeAfter.atZone(zoneId).toInstant().toEpochMilli(),
                                    alarmItem.timeZone,
                                    alarmItem.id)
                        }

                        alarmList.groupBy {
                            val zoneId = ZoneId.of(it.timeZone)
                            zoneId.rules.nextTransition(Instant.now()).dateTimeAfter.atZone(zoneId).toInstant().toEpochMilli()
                        }.forEach {
                            if(it.value.any { alarmItem -> alarmItem.on_off == 1 })
                                dstController.scheduleNextDaylightSavingTimeAlarm(it.key)
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
}
