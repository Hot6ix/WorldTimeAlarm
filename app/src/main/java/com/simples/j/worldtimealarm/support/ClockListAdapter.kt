package com.simples.j.worldtimealarm.support

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.util.TimeFormatException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.clock_list_item.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.exp

/**
 * Created by j on 19/02/2018.
 *
 */
class ClockListAdapter(private var context: Context, private var list: ArrayList<ClockItem>, private var calendar: Calendar): RecyclerView.Adapter<ClockListAdapter.ViewHolder>() {

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.clock_list_item, parent, false))
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeZone = TimeZone.getTimeZone(list[holder.adapterPosition].timezone?.replace(" ", "_"))
        val expectedCalendar = Calendar.getInstance()
        expectedCalendar.timeInMillis = calendar.timeInMillis

        val differenceOriginal = timeZone.getOffset(System.currentTimeMillis()) - calendar.timeZone.getOffset(System.currentTimeMillis())
        expectedCalendar.add(Calendar.MILLISECOND, differenceOriginal)

        val offset = if(calendar.timeZone == timeZone) context.resources.getString(R.string.same_as_set)
        else MediaCursor.getOffsetOfDifference(context, differenceOriginal, MediaCursor.TYPE_CONVERTER)

        holder.timeZoneName.text = timeZone.id.replace("_", " ")
        holder.timeZoneOffset.text = offset

        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        holder.amPm.text = if(expectedCalendar.get(Calendar.AM_PM) == 0) context.getString(R.string.am) else context.getString(R.string.pm)
        holder.timeZoneTime.text = timeFormat.format(expectedCalendar.time)
        holder.timeZoneDate.text = DateUtils.formatDateTime(context, expectedCalendar.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY)
    }

    fun removeItem(index: Int) {
        list.removeAt(index)
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, itemCount)
    }

    fun addItem(index: Int, item: ClockItem) {
        list.add(index, item)
        notifyItemInserted(index)
        notifyItemRangeChanged(index, itemCount)
    }

    fun setOnItemListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var timeZoneName: TextView = view.time_zone_name_clock
        var timeZoneOffset: TextView = view.time_zone_offset
        var timeZoneTime: TextView = view.time_zone_time
        var amPm: TextView = view.time_zone_am_pm
        var timeZoneDate: TextView = view.time_zone_date
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, item: AlarmItem)
        fun onItemStatusChanged(b: Boolean, item: AlarmItem)
    }
}