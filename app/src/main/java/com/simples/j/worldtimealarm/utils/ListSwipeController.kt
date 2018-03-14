package com.simples.j.worldtimealarm.utils

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

/**
 * Created by j on 03/03/2018.
 *
 */
class ListSwipeController: ItemTouchHelper.Callback() {

    private lateinit var listener: OnSwipeListener

    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int = makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwipe(viewHolder, direction)
    }

    fun setOnSwipeListener(listener: OnSwipeListener) {
        this.listener = listener
    }

    interface OnSwipeListener {
        fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }

}