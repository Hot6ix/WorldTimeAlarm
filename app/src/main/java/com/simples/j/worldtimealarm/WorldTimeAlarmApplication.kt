package com.simples.j.worldtimealarm

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class WorldTimeAlarmApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
    }
}