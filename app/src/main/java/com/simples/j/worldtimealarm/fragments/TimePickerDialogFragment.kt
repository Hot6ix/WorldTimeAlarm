package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.models.WorldClockViewModel
import org.threeten.bp.ZonedDateTime


class TimePickerDialogFragment: DialogFragment() {

    private lateinit var dialog: TimePickerDialog
    private lateinit var viewModel: WorldClockViewModel
    private var listener: TimePickerDialog.OnTimeSetListener? = null
    private val now = ZonedDateTime.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.run {
            viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = TimePickerDialog(
                context,
                listener,
                viewModel.mainZonedDateTime.value?.hour ?: now.hour,
                viewModel.mainZonedDateTime.value?.minute ?: now.minute,
                false
        )

        return dialog
    }

    fun setTimeSetListener(listener: TimePickerDialog.OnTimeSetListener) {
        this.listener = listener
    }

    companion object {
        fun newInstance() = TimePickerDialogFragment()
    }
}