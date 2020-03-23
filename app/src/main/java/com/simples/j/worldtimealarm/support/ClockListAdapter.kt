package com.simples.j.worldtimealarm.support

import android.content.Context
import android.icu.util.TimeZone
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.models.WorldClockViewModel
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.clock_list_item.view.*
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by j on 19/02/2018.
 *
 */
class ClockListAdapter(private var context: Context, private var list: ArrayList<ClockItem>, private var viweModel: WorldClockViewModel): RecyclerView.Adapter<ClockListAdapter.ViewHolder>() {

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
            viweModel.mainZonedDateTime.value?.let {
                val target = it.withZoneSameInstant(ZoneId.of(item))

                val targetOffset = target.zone.rules.getOffset(it.toInstant())
                val mainOffset = it.offset

                val difference = (targetOffset.totalSeconds - mainOffset.totalSeconds) * 1000

                val offset =
                        if(target.zone == it.zone) context.resources.getString(R.string.same_as_set)
                        else MediaCursor.getOffsetOfDifference(context, difference, MediaCursor.TYPE_CONVERTER)

                holder.timeZoneName.text =
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) MediaCursor.getBestNameForTimeZone(TimeZone.getTimeZone(target.zone.id))
                        else target.zone.id
                val targetTimeZone = java.util.TimeZone.getTimeZone(target.zone.id)
                if(targetTimeZone.useDaylightTime() && targetTimeZone.inDaylightTime(Date(target.toInstant().toEpochMilli()))) {
                    holder.timeZoneDst.visibility = View.VISIBLE
                }
                else {
                    holder.timeZoneDst.visibility = View.GONE
                }
                holder.timeZoneOffset.text = offset

                holder.timeZoneTime.text = target.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                holder.timeZoneDate.text = target.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
            }
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
        var timeZoneDst: ImageView = view.time_zone_dst
        var timeZoneOffset: TextView = view.time_zone_offset
        var timeZoneTime: TextView = view.time_zone_time
        var timeZoneDate: TextView = view.time_zone_date
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, item: AlarmItem)
    }
}