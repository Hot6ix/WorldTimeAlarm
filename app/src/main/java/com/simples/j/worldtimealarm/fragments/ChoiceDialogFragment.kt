package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
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

    private var listener: OnDialogEventListener? = null
    private var lastChoice: Int = -1
    private var currentChoice: Int = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val array = arguments?.getStringArray(CONTENT_ARRAY)
        if(savedInstanceState != null) {
            currentChoice = savedInstanceState.getInt(CURRENT_CHOICE)
        }

        val dialog = AlertDialog.Builder(context!!)
                .setTitle(arguments?.getString(CONTENT_TITLE))
                .setSingleChoiceItems(array, lastChoice) { dialogInterface, index ->
                    listener?.onItemSelect(dialogInterface, index)
                    currentChoice = index
                }
                .setPositiveButton(R.string.ok) { dialogInterface, _ ->
                    listener?.onPositiveButtonClick(dialogInterface, currentChoice)
                }
                .setNegativeButton(R.string.cancel) { dialogInterface, index ->
                    listener?.onNegativeButtonClick(dialogInterface, index)
                }
        return dialog.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(CURRENT_CHOICE, currentChoice)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        listener?.onDialogDismiss(dialog)
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
       const val CURRENT_CHOICE = "CURRENT_CHOICE"
   }

}