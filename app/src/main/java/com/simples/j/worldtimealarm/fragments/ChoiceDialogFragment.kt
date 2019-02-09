package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.interfaces.OnDialogEventListener

/**
 * Created by j on 07/03/2018.
 *
 */
class ChoiceDialogFragment: DialogFragment() {

    private lateinit var listener: OnDialogEventListener
    private var lastChoice: Int = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(C.TAG, "Dialog created")
        val array = arguments.getStringArray(CONTENT_ARRAY)
        var selected = lastChoice

        val dialog = AlertDialog.Builder(activity)
                .setTitle(arguments.getString(CONTENT_TITLE))
                .setSingleChoiceItems(array, selected, { dialogInterface, index ->
                    listener.onItemSelected(dialogInterface, index)
                    selected = lastChoice
                })
                .setNegativeButton(R.string.cancel, { dialogInterface, index ->
                    listener.onNegativeButtonClickListener(dialogInterface, index)
                })
                .setPositiveButton(R.string.ok, { dialogInterface, index ->
                    listener.onPositiveButtonClickListener(dialogInterface, index)
                })
        return dialog.create()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        listener.onDialogDismissListener(dialog)
        super.onDismiss(dialog)
    }

    fun setLastChoice(lastChoice: Int) {
        this.lastChoice = lastChoice
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

   companion object {

       fun newInstance(title: String, array: Array<String>): ChoiceDialogFragment {
           val instance = ChoiceDialogFragment()
           val bundle = Bundle()
           bundle.putString(CONTENT_TITLE, title)
           bundle.putStringArray(CONTENT_ARRAY, array)
           instance.arguments = bundle

           return instance
       }

       const val CONTENT_TITLE = "CONTENT_TITLE"
       const val CONTENT_ARRAY = "CONTENT_ARRAY"

   }

}