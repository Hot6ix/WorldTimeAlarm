package com.simples.j.worldtimealarm.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.simples.j.worldtimealarm.etc.*
import com.simples.j.worldtimealarm.utils.AlarmController
import java.util.*
import kotlin.collections.ArrayList

class AlarmGeneratorViewModel(app: Application): AndroidViewModel(app) {

    val alarmController: AlarmController = AlarmController.getInstance()

    var optionList: ArrayList<OptionItem> = ArrayList()

    val alarmItem: MutableLiveData<AlarmItem> by lazy {
        MutableLiveData<AlarmItem>()
    }

    val timeZone: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    var calendar: Calendar = Calendar.getInstance()

    val startDate: MutableLiveData<Calendar> by lazy {
        MutableLiveData<Calendar>()
    }

    val endDate: MutableLiveData<Calendar> by lazy {
        MutableLiveData<Calendar>()
    }

    val recurrences: MutableLiveData<IntArray> by lazy {
        MutableLiveData<IntArray>()
    }

    val ringtone: MutableLiveData<RingtoneItem> by lazy {
        MutableLiveData<RingtoneItem>()
    }

    val vibration: MutableLiveData<PatternItem> by lazy {
        MutableLiveData<PatternItem>()
    }

    val snooze: MutableLiveData<SnoozeItem> by lazy {
        MutableLiveData<SnoozeItem>()
    }

    val label: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val colorTag: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

}