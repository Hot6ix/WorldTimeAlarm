package com.simples.j.worldtimealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeSetChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val requestIntent = Intent(MainActivity.ACTION_UPDATE_ALL)
                context.sendBroadcast(requestIntent)
            }
        }
//        if(intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
//            Toast.makeText(context, "TimeZone Changed.", Toast.LENGTH_SHORT).show()
//            val requestIntent = Intent(MainActivity.ACTION_UPDATE_ALL)
//            context.sendBroadcast(requestIntent)
//            isTimeZoneChanged = true
//        }
    }

    companion object {
//        var isTimeZoneChanged = false
    }
}
