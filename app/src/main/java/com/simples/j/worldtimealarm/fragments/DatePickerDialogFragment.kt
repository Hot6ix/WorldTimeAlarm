package com.simples.j.worldtimealarm.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.models.WorldClockViewModel
import org.threeten.bp.ZonedDateTime

class DatePickerDialogFragment: DialogFragment() {

    private lateinit var dialog: DatePickerDialog
    private lateinit var fragmentContext: Context
    private lateinit var viewModel: WorldClockViewModel
    private var listener: DatePickerDialog.OnDateSetListener? = null
    private val now = ZonedDateTime.now()

    var minDate: Long = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.run {
            viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = DatePickerDialog(
                fragmentContext,
                listener,
                viewModel.mainZonedDateTime.value?.year ?: now.year,
                viewModel.mainZonedDateTime.value?.monthValue?.minus(1) ?: now.monthValue,
                viewModel.mainZonedDateTime.value?.dayOfMonth ?: now.dayOfMonth
        )

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

    companion object {
        fun newInstance() = DatePickerDialogFragment()

        const val CURRENT_MIN_DATE = "CURRENT_MIN_DATE"
    }
}