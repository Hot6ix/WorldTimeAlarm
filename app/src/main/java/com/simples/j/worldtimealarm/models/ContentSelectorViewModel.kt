package com.simples.j.worldtimealarm.models

import android.media.Ringtone
import androidx.lifecycle.ViewModel

class ContentSelectorViewModel : ViewModel() {

    var action: String? = null
    var lastSelectedValue: Any? = null

    var ringtone: Ringtone? = null

}
