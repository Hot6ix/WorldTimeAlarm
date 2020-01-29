package com.simples.j.worldtimealarm.utils

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper

/**
 * Created by j on 03/03/2018.
 *
 */
class ListSwipeController(): ItemTouchHelper.Callback() {

    private lateinit var listener: OnListControlListener

//    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int = makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START or ItemTouchHelper.END)

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        listener.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwipe(viewHolder, direction)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true

    fun setOnSwipeListener(listener: OnListControlListener) {
        this.listener = listener
    }

    interface OnListControlListener {
        fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int)
        fun onItemMove(from: Int, to: Int)
    }

}