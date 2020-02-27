package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.simples.j.worldtimealarm.BuildConfig
import com.simples.j.worldtimealarm.etc.C
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

                        list.forEach {
                            it.index = list.indexOf(it)
                            db.updateAlarmIndex(it)
                        }
                    }
                }
            }
            else -> {

            }
        }
    }
}
