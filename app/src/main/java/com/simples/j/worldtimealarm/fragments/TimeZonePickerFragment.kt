package com.simples.j.worldtimealarm.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.simples.j.worldtimealarm.R

class TimeZonePickerFragment : Fragment() {

    private var listener: OnTimeZoneChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_zone_picker, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTimeZoneChangeListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnTimeZoneChangeListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun setListener(listener: OnTimeZoneChangeListener) {
        this.listener = listener
    }

    interface OnTimeZoneChangeListener {
        fun onTimeZoneChanged(timeZone: String)
        fun onCountryChanged(country: String)
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimeZonePickerFragment()
    }
}
