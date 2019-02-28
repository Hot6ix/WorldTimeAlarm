package com.simples.j.worldtimealarm.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import java.util.*

class DatePickerDialogFragment: DialogFragment() {

    private lateinit var dialog: DatePickerDialog
    private var listener: DatePickerDialog.OnDateSetListener? = null

    var calendar: Calendar = Calendar.getInstance()
    var minDate: Long = calendar.timeInMillis

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = DatePickerDialog(context!!, listener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        if(savedInstanceState != null) {
            minDate = savedInstanceState.getLong(CURRENT_MIN_DATE)
        }

        dialog.datePicker.minDate = minDate
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(CURRENT_MIN_DATE, minDate)
    }

    fun setDateSetListener(listener: DatePickerDialog.OnDateSetListener) {
        this.listener = listener
    }

    fun setDate(calendar: Calendar) {
        this.calendar = calendar
    }

    companion object {
        fun newInstance() = DatePickerDialogFragment()

        const val CURRENT_MIN_DATE = "CURRENT_MIN_DATE"
    }
}