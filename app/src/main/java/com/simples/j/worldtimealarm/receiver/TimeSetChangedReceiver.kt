package com.simples.j.worldtimealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.utils.DstController

class TimeSetChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED  -> {
                DstController(context).handleSystemDst()

                val requestIntent = Intent(MainActivity.ACTION_UPDATE_ALL)
                context.sendBroadcast(requestIntent)
            }
        }
    }
}
