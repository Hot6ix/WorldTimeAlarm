package com.simples.j.worldtimealarm.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.threeten.bp.ZonedDateTime

class WorldClockViewModel(app: Application): AndroidViewModel(app) {

    val mainZonedDateTime: MutableLiveData<ZonedDateTime> by lazy {
        MutableLiveData<ZonedDateTime>().also {
            it.value = ZonedDateTime.now()
        }
    }

}