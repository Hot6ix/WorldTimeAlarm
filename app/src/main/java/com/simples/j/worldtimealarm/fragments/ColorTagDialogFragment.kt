package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.support.ColorGridAdapter

class ColorTagDialogFragment: DialogFragment(), ColorGridAdapter.OnItemClickListener {

    private var listener: OnDialogEventListener? = null
    private var lastChoice: Int = 0
    private var currentChoice: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val colorPickerView = View.inflate(context, R.layout.color_tag_dialog_view, null)
        val colorView: RecyclerView = colorPickerView.findViewById(R.id.colorPicker)
        val adapter = ColorGridAdapter(context!!, lastChoice)
        adapter.setOnItemClickListener(this)
        adapter.setSelected(lastChoice)

        if(savedInstanceState != null) {
            currentChoice = savedInstanceState.getInt(CURRENT_CHOICE)
            adapter.setSelected(currentChoice)
        }

        colorView.layoutManager = GridLayoutManager(context, 5)
        colorView.adapter = adapter

        val dialog = AlertDialog.Builder(context!!)
                .setTitle(R.string.select_color)
                .setView(colorPickerView)
                .setPositiveButton(resources.getString(R.string.ok)) { dialogInterface, _ ->
                    listener?.onPositiveButtonClick(dialogInterface, currentChoice)
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, index ->
                    listener?.onNegativeButtonClick(dialogInterface, index)
                }
                .setNeutralButton(resources.getString(R.string.clear)) { dialogInterface, index ->
                    listener?.onNeutralButtonClick(dialogInterface, index)
                    currentChoice = 0
                }
        return dialog.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(ChoiceDialogFragment.CURRENT_CHOICE, currentChoice)
    }

    override fun onColorItemClick(color: Int, view: View) {
        currentChoice = color
    }

    fun setLastChoice(lastChoice: Int) {
        this.lastChoice = lastChoice
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

    interface OnDialogEventListener {
        fun onPositiveButtonClick(inter: DialogInterface, color: Int)
        fun onNegativeButtonClick(inter: DialogInterface, index: Int)
        fun onNeutralButtonClick(inter: DialogInterface, index: Int)
    }

    companion object {
        fun newInstance() = ColorTagDialogFragment()

        const val CURRENT_CHOICE = "CURRENT_CHOICE"
    }
}