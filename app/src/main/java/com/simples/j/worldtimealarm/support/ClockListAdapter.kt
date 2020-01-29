package com.simples.j.worldtimealarm.support

import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.clock_list_item.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

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

    override fun getItemId(position: Int): Long = list[position].hashCode().toLong()

    override fun getItemViewType(position: Int): Int = 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[holder.adapterPosition].timezone?.replace(" ", "_")
        if(item.isNullOrEmpty()) {
            holder.timeZoneName.text = context.getString(R.string.time_zone_wrong)
            holder.timeZoneOffset.visibility = View.GONE
        }
        else {
            val timeZone = TimeZone.getTimeZone(item)
            val expectedCalendar = Calendar.getInstance(timeZone)
            expectedCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

            val difference = expectedCalendar.timeZone.getOffset(System.currentTimeMillis()) - calendar.timeZone.getOffset(System.currentTimeMillis())
            expectedCalendar.add(Calendar.MILLISECOND, difference)

            val offset =
                    if(calendar.timeZone == timeZone) context.resources.getString(R.string.same_as_set)
                    else MediaCursor.getOffsetOfDifference(context, difference, MediaCursor.TYPE_CONVERTER)

            holder.timeZoneName.text =
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(expectedCalendar.timeZone.id))
                    else expectedCalendar.timeZone.id
            holder.timeZoneOffset.text = offset

            val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
            timeFormat.timeZone = timeZone
            val dateFormat = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault())
            dateFormat.timeZone = timeZone
            holder.amPm.text = if(expectedCalendar.get(Calendar.AM_PM) == 0) context.getString(R.string.am) else context.getString(R.string.pm)
            holder.timeZoneTime.text = timeFormat.format(expectedCalendar.time)
            holder.timeZoneDate.text = dateFormat.format(expectedCalendar.time)
        }
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
    }
}