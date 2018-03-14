package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import com.simples.j.worldtimealarm.R


/**
 * Created by j on 12/03/2018.
 *
 */
class ListItemDecorator(private var context: Context, ori: Int, private var divider: Drawable): DividerItemDecoration(context, ori) {

    override fun onDraw(c: Canvas?, parent: RecyclerView, state: RecyclerView.State?) {
        val left = context.resources.getDimensionPixelSize(R.dimen.padding20)
        val right = parent.width - context.resources.getDimensionPixelSize(R.dimen.padding20)

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

}