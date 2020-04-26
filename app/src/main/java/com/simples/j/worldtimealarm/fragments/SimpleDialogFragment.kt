package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.simples.j.worldtimealarm.R

class SimpleDialogFragment: DialogFragment() {

    private var listener: OnDialogEventListener? = null
    private var title: String? = null
    private var message: String? = null
    private var cancelableValue: Int = CANCELABLE_ALL
    private var neutralButtonTitle: String? = null

    private lateinit var fragmentContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            title = it.getString(TITLE)
            message = it.getString(MESSAGE)
            cancelableValue = it.getInt(CANCELABLE)
            neutralButtonTitle = it.getString(NEUTRAL)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = AlertDialog.Builder(fragmentContext)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(resources.getString(R.string.ok)) { dialogInterface, _ ->
                    listener?.onPositiveButtonClicked(dialogInterface)
                }

        if(cancelableValue == CANCELABLE_ALL) {
            dialog.setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ ->
                listener?.onNegativeButtonClicked(dialogInterface)
            }
        }

        neutralButtonTitle?.let {
            dialog.setNeutralButton(it) { dialogInterface, _ ->
                listener?.onNeutralButtonClicked(dialogInterface)
            }
        }

        return dialog.create().apply {
            if(cancelableValue == CANCELABLE_ALL || cancelableValue == CANCELABLE_NO_BUTTON) {
                setCancelable(true)
                setCanceledOnTouchOutside(true)
            }
            else {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
        }
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

    interface OnDialogEventListener {
        fun onPositiveButtonClicked(inter: DialogInterface)
        fun onNegativeButtonClicked(inter: DialogInterface)
        fun onNeutralButtonClicked(inter: DialogInterface)
    }

    companion object {
        fun newInstance(title: String, msg: String, cancelable: Int = 0, neutralBtnTitle: String? = null) = SimpleDialogFragment().apply {
            arguments = Bundle().apply {
                putString(TITLE, title)
                putString(MESSAGE, msg)
                putInt(CANCELABLE, cancelable)
                putString(NEUTRAL, neutralBtnTitle)
            }
        }

        const val TITLE = "title"
        const val MESSAGE = "message"
        const val CANCELABLE = "cancelable"
        const val CANCELABLE_ALL = 0
        const val CANCELABLE_NO_BUTTON = 1
        const val NOT_CANCELABLE = 2
        const val NEUTRAL = "NEUTRAL"
        const val TAG = "SimpleDialogFragment"
    }
}