package com.simples.j.worldtimealarm.models

import android.media.Ringtone
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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

    var defaultStart: Long? = null
    var defaultEnd: Long? = null

}
