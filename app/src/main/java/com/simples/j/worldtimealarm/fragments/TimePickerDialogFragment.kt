package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*


class TimePickerDialogFragment: DialogFragment() {

    private lateinit var dialog: TimePickerDialog
    private var listener: TimePickerDialog.OnTimeSetListener? = null

    var calendar: Calendar = Calendar.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = TimePickerDialog(context, listener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

        return dialog
    }

    fun setTimeSetListener(listener: TimePickerDialog.OnTimeSetListener) {
        this.listener = listener
    }

    fun setTime(calendar: Calendar) {
        this.calendar = calendar
    }

    companion object {
        fun newInstance() = TimePickerDialogFragment()
    }
}