package com.simples.j.worldtimealarm.support

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import kotlinx.android.synthetic.main.alarm_list_item.view.*
import java.lang.NumberFormatException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by j on 19/02/2018.
 *
 */
class AlarmListAdapter(var list: ArrayList<AlarmItem>, var context: Context): RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.alarm_list_item, parent, false))
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[holder.adapterPosition]

        val calendar = Calendar.getInstance()
        calendar.time = Date(item.timeSet.toLong())
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (calendar.timeInMillis - System.currentTimeMillis() > C.ONE_DAY) {
            calendar.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        }

        if(item.startDate == null && item.endDate == null) {
            holder.range.visibility = View.GONE
        }
        else {
            val start = try {
                Calendar.getInstance().apply {
                    item.startDate?.let {
                        if(it > 0) timeInMillis = it
                        else throw NumberFormatException("Invalid value for calendar")
                    }
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                null
            }

            val end = try {
                Calendar.getInstance().apply {
                    item.endDate?.let {
                        if(it > 0) timeInMillis = it
                        else throw NumberFormatException("Invalid value for calendar")
                    }
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                null
            }

            val dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT)

            val rangeText = when {
                start != null && end != null -> {
                    context.getString(R.string.range_text).format(dateFormatter.format(start.time), dateFormatter.format(end.time))
                }
                start != null -> {
                    context.getString(R.string.range_begin).format(dateFormatter.format(start.time))
                }
                end != null -> {
                    context.getString(R.string.range_until).format(dateFormatter.format(end.time))
                }
                else -> {
                    holder.range.visibility = View.GONE
                    null
                }
            }
            holder.range.text = rangeText
        }

        val colorTag = item.colorTag
        if(colorTag != 0) {
            holder.colorTag.visibility = View.VISIBLE
            holder.colorTag.setBackgroundColor(colorTag)
        }
        else {
            holder.colorTag.visibility = View.GONE
        }

        holder.amPm.text = if(calendar.get(Calendar.AM_PM) == 0) context.getString(R.string.am) else context.getString(R.string.pm)
        holder.localTime.text = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar.time)

        val dayArray = context.resources.getStringArray(R.array.day_of_week_simple)
        val dayLongArray = context.resources.getStringArray(R.array.day_of_week_full)
        val repeatArray = item.repeat.mapIndexed { index, i ->
            if(i > 0) dayArray[index] else null
        }.filter { it != null }
        if(repeatArray.isNotEmpty()) {
            holder.repeat.text =
                    if(repeatArray.size == 7) context.resources.getString(R.string.everyday)
                    else if(repeatArray.contains(dayArray[6]) && repeatArray.contains(dayArray[0]) && repeatArray.size  == 2) context.resources.getString(R.string.weekend)
                    else if(repeatArray.contains(dayArray[1]) && repeatArray.contains(dayArray[2]) && repeatArray.contains(dayArray[3]) && repeatArray.contains(dayArray[4]) && repeatArray.contains(dayArray[5]) && repeatArray.size == 5) context.resources.getString(R.string.weekday)
                    else if(repeatArray.size == 1) dayLongArray[item.repeat.indexOf(item.repeat.find { it > 0 }!!)]
                    else repeatArray.joinToString()
        }
        else {
            if(calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                holder.repeat.text = context.resources.getString(R.string.today)
            else
                holder.repeat.text = context.resources.getString(R.string.tomorrow)
        }

        holder.itemView.setOnClickListener { listener.onItemClicked(it, item) }
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.on_off != 0

        if(item.on_off != 0) {
            holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
        }
        else {
            holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
        }

        holder.switch.setOnCheckedChangeListener { _, b ->
            if(!b) {
                holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
                holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
                holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
                holder.range.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            }
            else {
                holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
                holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
                holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
                holder.range.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            }

            listener.onItemStatusChanged(b, item)
        }
    }
    fun removeItem(index: Int) {
        list.removeAt(index)
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, itemCount)
    }

    fun addItem(index: Int, item: AlarmItem) {
        list.add(index, item)
        notifyItemInserted(index)
        notifyItemRangeChanged(index, itemCount)
    }

    fun setOnItemListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var amPm: TextView = view.am_pm
        var localTime: TextView = view.local_time
        var repeat: TextView = view.repeat
        var switch: Switch = view.on_off
        var colorTag: View = view.colorTag
        var range: TextView = view.range
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, item: AlarmItem)
        fun onItemStatusChanged(b: Boolean, item: AlarmItem)
    }
}