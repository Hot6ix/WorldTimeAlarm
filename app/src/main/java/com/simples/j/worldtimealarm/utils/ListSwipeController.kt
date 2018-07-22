package com.simples.j.worldtimealarm.utils

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.simples.j.worldtimealarm.interfaces.ItemTouchHelperAdapter

/**
 * Created by j on 03/03/2018.
 *
 */
class ListSwipeController(val adapter: ItemTouchHelperAdapter): ItemTouchHelper.Callback() {

    private lateinit var listener: OnSwipeListener

    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int = makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)
//    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START or ItemTouchHelper.END)

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
        adapter.onItemMove(viewHolder!!.adapterPosition, target!!.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwipe(viewHolder, direction)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true

    fun setOnSwipeListener(listener: OnSwipeListener) {
        this.listener = listener
    }

    interface OnSwipeListener {
        fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }

}