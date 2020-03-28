package com.simples.j.worldtimealarm.models

import android.media.Ringtone
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class ContentSelectorViewModel : ViewModel() {

    var action: String? = null
    var lastSelectedValue: Any? = null

    var ringtone: Ringtone? = null

    var currentTab: Int = 0
    val startDate: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>()
    }
    val endDate: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>()
    }

    var timeZone: String = TimeZone.getDefault().id
}
