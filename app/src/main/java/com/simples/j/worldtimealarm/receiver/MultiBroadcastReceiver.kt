package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.simples.j.worldtimealarm.BuildConfig
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor

class MultiBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(C.TAG, "${intent.action}")

        when(intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {

                when(BuildConfig.VERSION_CODE) {
                    22 -> {
                        val db = DatabaseCursor(context)
                        val list = db.getAlarmList()
                        val alarmController = AlarmController.getInstance()

                        list.forEach {
                            it.index = list.indexOf(it)
                            it.pickerTime = it.timeSet.toLong()

                            db.updateAlarmIndex(it)

                            if(it.on_off == 1) {
                                alarmController.cancelAlarm(context, it.notiId)
                                alarmController.scheduleLocalAlarm(context, it, AlarmController.TYPE_ALARM)
                            }
                        }

                        val preference = PreferenceManager.getDefaultSharedPreferences(context)
                        preference.edit()
                                .putBoolean(context.getString(R.string.setting_converter_timezone_key), true)
                                .apply()
                    }
                }
            }
            else -> {

            }
        }
    }
}
