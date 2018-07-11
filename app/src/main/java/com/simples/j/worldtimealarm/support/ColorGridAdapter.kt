package com.simples.j.worldtimealarm.support

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import com.simples.j.worldtimealarm.R
import kotlinx.android.synthetic.main.alarm_day_item.view.*
import kotlinx.android.synthetic.main.color_tag_item.view.*

class ColorGridAdapter(private val context: Context, private val defaultColor: Int): RecyclerView.Adapter<ColorGridAdapter.ViewHolder>() {

    private val colorList = context.resources.getIntArray(R.array.color_list)
    private var lastChecked: Button? = null
    private lateinit var colorItemClickListener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorGridAdapter.ViewHolder {
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

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var layout = view.color_layout
        var color = view.color
    }

    interface OnItemClickListener {
        fun onColorItemClick(color: Int, view: View)
    }

}