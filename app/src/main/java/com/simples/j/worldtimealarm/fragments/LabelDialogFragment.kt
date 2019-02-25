package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.simples.j.worldtimealarm.R

class LabelDialogFragment: DialogFragment() {

    private var listener: OnDialogEventListener? = null
    private var lastLabel: String = ""
    private var currentLabel: String = ""

    private lateinit var labelEditor: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(savedInstanceState != null) {
            currentLabel = savedInstanceState.getString(CURRENT_LABEL, "")
        }

        val labelView = View.inflate(context, R.layout.label_dialog_view, null)
        labelEditor = labelView.findViewById(R.id.label)
        labelEditor.setText(lastLabel)
        labelEditor.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable) {
                currentLabel = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        val dialog = AlertDialog.Builder(context!!)
                .setTitle(resources.getString(R.string.label))
                .setView(labelView)
                .setPositiveButton(resources.getString(R.string.ok)) { dialogInterface, _ ->
                    listener?.onPositiveButtonClick(dialogInterface, currentLabel)
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ ->
                    listener?.onNegativeButtonClick(dialogInterface)
                }
                .setNeutralButton(resources.getString(R.string.clear)) { dialogInterface, _ ->
                    currentLabel = ""
                    labelEditor.setText("")
                    listener?.onNeutralButtonClick(dialogInterface)
                }
                .setOnCancelListener { currentLabel = lastLabel }

        return dialog.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(CURRENT_LABEL, labelEditor.text.toString())
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

    fun setLastLabel(label: String) {
        this.lastLabel = label
    }

    interface OnDialogEventListener {
        fun onPositiveButtonClick(inter: DialogInterface, label: String)
        fun onNegativeButtonClick(inter: DialogInterface)
        fun onNeutralButtonClick(inter: DialogInterface)
    }

    companion object {
        fun newInstance() = LabelDialogFragment()

        const val CURRENT_LABEL = "CURRENT_LABEL"
    }
}