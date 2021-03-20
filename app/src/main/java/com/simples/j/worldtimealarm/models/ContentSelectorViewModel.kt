package com.simples.j.worldtimealarm.models

import android.media.Ringtone
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.util.*

class ContentSelectorViewModel : ViewModel() {

    var action: String? = null
    var lastSelectedValue: Any? = null

    var ringtone: Ringtone? = null

    var startDate: LocalDate? = null
    var endDate: LocalDate? = null

    var timeZone: String = TimeZone.getDefault().id
}
