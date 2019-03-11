package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.interfaces.OnDialogEventListener

class SnoozeDialogFragment: DialogFragment() {

    private var listener: OnDialogEventListener? = null
    private var lastChoice: Int = 0
    private var currentChoice: Int = 0
    private lateinit var snoozeSeekBar: SeekBar
    private lateinit var snoozeValues: Array<Long>
    private lateinit var snoozeTimeList: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        snoozeValues = resources.getIntArray(R.array.snooze_values).map { it.toLong() }.toTypedArray()
        snoozeTimeList = resources.getStringArray(R.array.snooze_array)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(savedInstanceState != null) {
            currentChoice = savedInstanceState.getInt(CURRENT_CHOICE)
        }

        val snoozeView = View.inflate(context, R.layout.snooze_dialog_view, null)
        val snoozeTime = snoozeView.findViewById<TextView>(R.id.snooze_time)
        snoozeTime.text = snoozeTimeList[lastChoice]
        snoozeSeekBar = snoozeView.findViewById(R.id.snooze)
        snoozeSeekBar.progress = lastChoice
        snoozeSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                listener?.onItemSelect(null, progress)
                currentChoice = progress
                snoozeTime.text = snoozeTimeList[progress]
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(context!!)
                .setView(snoozeView)
                .setPositiveButton(resources.getString(R.string.ok)) { dialogInterface, _ ->
                    listener?.onPositiveButtonClick(dialogInterface, currentChoice)
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, index ->
                    listener?.onNegativeButtonClick(dialogInterface, index)
                }
        return dialog.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(ChoiceDialogFragment.CURRENT_CHOICE, currentChoice)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        listener?.onDialogDismiss(dialog)
        super.onDismiss(dialog)
    }

    fun setLastChoice(lastChoice: Int) {
        this.lastChoice = lastChoice
        this.currentChoice = lastChoice
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

    companion object {
        fun newInstance() = SnoozeDialogFragment()

        const val CURRENT_CHOICE = "CURRENT_CHOICE"
    }
}