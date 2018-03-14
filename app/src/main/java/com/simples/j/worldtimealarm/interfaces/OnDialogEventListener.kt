package com.simples.j.worldtimealarm.interfaces

import android.content.DialogInterface

/**
 * Created by j on 07/03/2018.
 *
 */
interface OnDialogEventListener {
    fun onItemSelected(inter: DialogInterface, index: Int)
    fun onPositiveButtonClickListener(inter: DialogInterface, index: Int)
    fun onNegativeButtonClickListener(inter: DialogInterface, index: Int)
    fun onDialogDismissListener(inter: DialogInterface?)
}