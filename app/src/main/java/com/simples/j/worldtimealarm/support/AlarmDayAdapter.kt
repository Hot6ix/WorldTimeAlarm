package com.simples.j.worldtimealarm.support

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.simples.j.worldtimealarm.R
import kotlinx.android.synthetic.main.alarm_day_item.view.*

/**
 * Created by j on 19/02/2018.
 *
 */
class AlarmDayAdapter(private var selectedDays: IntArray, private var context: Context): RecyclerView.Adapter<AlarmDayAdapter.ViewHolder>() {

    private lateinit var onItemClickListener: OnItemClickListener
    private val arrayOfDay = context.resources.getStringArray(R.array.day_of_week)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.alarm_day_item, parent, false))
    }

    override fun getItemCount() = arrayOfDay.size

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = position

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.day.text = arrayOfDay[position]
        when(position) {
            0 -> holder.day.setTextColor(ContextCompat.getColorStateList(context, R.color.day_text_color_selector_sun))
            6 -> holder.day.setTextColor(ContextCompat.getColorStateList(context, R.color.day_text_color_selector_sat))
        }
        holder.day.isSelected = selectedDays[position] != 0
        holder.day.setOnClickListener {
            it.isSelected = !it.isSelected
            onItemClickListener.onDayItemSelected(it, position)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var day: Button = view.day_item
    }

    interface OnItemClickListener {
        fun onDayItemSelected(view: View, position: Int)
    }
}