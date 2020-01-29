package com.simples.j.worldtimealarm.support

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import kotlinx.android.synthetic.main.color_tag_item.view.*

class ColorGridAdapter(context: Context, private var defaultColor: Int): RecyclerView.Adapter<ColorGridAdapter.ViewHolder>() {

    private val colorList = context.resources.getIntArray(R.array.color_list)
    private var lastChecked: Button? = null
    private lateinit var colorItemClickListener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.color_tag_item, parent, false))
    }

    override fun getItemCount(): Int = colorList.size

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = position

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(colorList[position] == defaultColor) {
            lastChecked = holder.color
            holder.color.isSelected = true
        }

        holder.layout.setBackgroundColor(colorList[position])
        holder.color.setOnClickListener {
            colorItemClickListener.onColorItemClick(colorList[position], holder.itemView)
            if(lastChecked != null) {
                if(lastChecked != holder.color) {
                    lastChecked?.isSelected = false
                }
            }
            lastChecked = holder.color
            holder.color.isSelected = true
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.colorItemClickListener = listener
    }

    fun setSelected(color: Int) {
        defaultColor = color
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var layout: ConstraintLayout = view.color_layout
        var color: Button = view.color
    }

    interface OnItemClickListener {
        fun onColorItemClick(color: Int, view: View)
    }

}