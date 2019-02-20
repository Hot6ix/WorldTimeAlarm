package com.simples.j.worldtimealarm.interfaces

import android.content.DialogInterface

/**
 * Created by j on 07/03/2018.
 *
 */
interface OnDialogEventListener {
    fun onItemSelect(inter: DialogInterface?, index: Int)
    fun onPositiveButtonClick(inter: DialogInterface, index: Int)
    fun onNegativeButtonClick(inter: DialogInterface, index: Int)
    fun onDialogDismiss(inter: DialogInterface?)
}