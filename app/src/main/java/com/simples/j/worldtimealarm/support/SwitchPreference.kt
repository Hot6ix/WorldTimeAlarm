package com.simples.j.worldtimealarm.support

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.Switch
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.simples.j.worldtimealarm.R

class SwitchPreference: SwitchPreference {

    private var switch: Switch? = null
    private var switchListener: CompoundButton.OnCheckedChangeListener? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val layout = holder?.itemView
        switch = layout?.findViewById(R.id.pref_switch)
        switch?.setOnCheckedChangeListener(switchListener)
        switch?.isChecked = isChecked
    }

    override fun onClick() {}

    fun setSwitchListener(listener: CompoundButton.OnCheckedChangeListener) {
        this.switchListener = listener
    }

}