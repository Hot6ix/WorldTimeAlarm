package com.simples.j.worldtimealarm.support

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.preference.PreferenceManager
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.alarm_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by j on 19/02/2018.
 *
 */
class AlarmListAdapter(private var list: ArrayList<AlarmItem>, private val context: Context): RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {

    private lateinit var listener: OnItemClickListener
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var highlightId: Int = -1
    private var prefManager = PreferenceManager.getDefaultSharedPreferences(context)
    private var applyDayRepetition = prefManager.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.alarm_list_item, parent, false))
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long = list[position].id?.toLong() ?: -1

    override fun getItemViewType(position: Int): Int = 0

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[holder.adapterPosition]

        if(highlightId == item.notiId) {
            Handler().postDelayed({
                val drawable = holder.mainView.background as RippleDrawable
                drawable.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
                drawable.state = holder.mainView.drawableState
            }, 500)
            highlightId = -1
        }

        val calendar = Calendar.getInstance()
        calendar.time = Date(item.timeSet.toLong())
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        startDate =
                item.startDate.let {
                    if(it != null && it > 0) {
                        Calendar.getInstance().apply {
                            timeInMillis = it
                        }
                    }
                    else null
                }

        endDate =
                item.endDate.let {
                    if(it != null && it > 0) {
                        Calendar.getInstance().apply {
                            timeInMillis = it
                        }
                    }
                    else null
                }

        if(startDate == null && endDate == null) {
            holder.range.visibility = View.GONE
            holder.switch.isEnabled = true
        }
        else {
            // alarm is one-time and start date is set
            // if start date passed, disable alarm
            startDate.let { date ->
                if(date != null && !item.repeat.any { it > 0 }) {
                    holder.switch.isEnabled = date.timeInMillis > System.currentTimeMillis()
                }
                else holder.switch.isEnabled = true
            }

            // disable switch if alarm is expired
            with(endDate) {
                if(this != null) {
                    val difference = this.timeInMillis - System.currentTimeMillis()
                    if(TimeUnit.MILLISECONDS.toDays(difference) < 7) {
                        val expect = try {
                            AlarmController.getInstance().calculateDate(item, AlarmController.TYPE_ALARM, applyDayRepetition)
                        } catch (e: IllegalStateException) {
                            null
                        }

                        holder.switch.isEnabled =
                                if(expect == null) false
                                else expect.timeInMillis <= this.timeInMillis
                    }
                    else holder.switch.isEnabled = true
                }
            }

            val rangeText = when {
                startDate != null && endDate != null -> {
                    DateUtils.formatDateRange(context, startDate!!.timeInMillis, endDate!!.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
                }
                startDate != null -> {
                    if(item.repeat.any { it > 0 }) context.getString(R.string.range_begin).format(DateUtils.formatDateTime(context, startDate!!.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                    else null
                }
                endDate != null -> {
                    context.getString(R.string.range_until).format(DateUtils.formatDateTime(context, endDate!!.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                }
                else -> {
                    null
                }
            }

            if(rangeText.isNullOrEmpty())
                holder.range.visibility = View.GONE
            else {
                holder.range.visibility = View.VISIBLE
                holder.range.text = rangeText
            }
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

        with(item.repeat) {
            if(this.any { it > 0 }) {
                val dayArray = context.resources.getStringArray(R.array.day_of_week_simple)
                val dayLongArray = context.resources.getStringArray(R.array.day_of_week_full)

                val difference = TimeZone.getTimeZone(item.timeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
                val itemCalendar = Calendar.getInstance().apply {
                    timeInMillis = item.timeSet.toLong()
                }
                val tmp = itemCalendar.clone() as Calendar
                tmp.add(Calendar.MILLISECOND, difference)
                val dayDiff = Math.abs(MediaCursor.getDayDifference(tmp, itemCalendar, true))

                // for support old version of app
                val repeat = item.repeat.mapIndexed { index, i -> if(i > 0) index + 1 else 0 }


                val repeatArray = repeat.mapIndexed { index, i ->
                    if(i > 0) {
                        var dayOfWeek = index

                        if(difference != 0 && !MediaCursor.isSameDay(tmp, itemCalendar) && applyDayRepetition) {
                            if (tmp.timeInMillis > itemCalendar.timeInMillis)
                                dayOfWeek -= dayDiff.toInt()
                            else if (tmp.timeInMillis < itemCalendar.timeInMillis)
                                dayOfWeek += dayDiff.toInt()
                        }

                        if(dayOfWeek > 6) dayOfWeek -= 7
                        if(dayOfWeek < 0) dayOfWeek += 7

                        dayArray[dayOfWeek]
                    } else null
                }.filter { it != null }

                holder.repeat.text =
                        if(repeatArray.size == 7) context.resources.getString(R.string.everyday)
                        else if(repeatArray.contains(dayArray[6]) && repeatArray.contains(dayArray[0]) && repeatArray.size  == 2) context.resources.getString(R.string.weekend)
                        else if(repeatArray.contains(dayArray[1]) && repeatArray.contains(dayArray[2]) && repeatArray.contains(dayArray[3]) && repeatArray.contains(dayArray[4]) && repeatArray.contains(dayArray[5]) && repeatArray.size == 5) context.resources.getString(R.string.weekday)
                        else if(repeatArray.size == 1) {
                            repeat.find { it > 0 }?.let {
                                var dayOfWeek = it - 1

                                if(difference != 0 && !MediaCursor.isSameDay(tmp, itemCalendar) && applyDayRepetition) {
                                    if (tmp.timeInMillis > itemCalendar.timeInMillis)
                                        dayOfWeek -= dayDiff.toInt()
                                    else if (tmp.timeInMillis < itemCalendar.timeInMillis)
                                        dayOfWeek += dayDiff.toInt()
                                }

                                if(dayOfWeek > 6) dayOfWeek -= 7
                                if(dayOfWeek < 0) dayOfWeek += 7

                                dayLongArray[dayOfWeek]
                            }
                        }
                        else repeatArray.joinToString()
            }
            else {
                holder.repeat.text =
                        when {
                            DateUtils.isToday(calendar.timeInMillis) -> context.resources.getString(R.string.today)
                            DateUtils.isToday(calendar.timeInMillis - DateUtils.DAY_IN_MILLIS) && startDate == null -> context.resources.getString(R.string.tomorrow) // this can make adapter to know calendar date is tomorrow
                            startDate != null -> DateUtils.formatDateTime(context, startDate!!.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                            else -> DateUtils.formatDateTime(context, calendar.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                        }
            }
        }

        holder.itemView.setOnClickListener { listener.onItemClicked(it, item) }
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.on_off != 0

        if(item.timeZone != TimeZone.getDefault().id)
            holder.timezone.visibility = View.VISIBLE
        else
            holder.timezone.visibility = View.GONE

        with(item.ringtone) {
            if(this != null && this.isNotEmpty() && this != "null") {
                holder.ringtone.visibility = View.VISIBLE
            }
            else
                holder.ringtone.visibility = View.GONE
        }

        with(item.vibration) {
            if(this != null && this.isNotEmpty()) {
                holder.vibration.visibility = View.VISIBLE
            }
            else
                holder.vibration.visibility = View.GONE
        }

        if(item.snooze > 0L) {
            holder.snooze.visibility = View.VISIBLE
        }
        else
            holder.snooze.visibility = View.GONE

        updateView(holder, item.on_off != 0)

        holder.switch.setOnCheckedChangeListener { _, b ->
            updateView(holder, b)
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

    fun setHighlightId(id: Int) {
        this.highlightId = id
    }

    fun readPreferences() {
        applyDayRepetition = prefManager.getBoolean(context.getString(R.string.setting_time_zone_affect_repetition_key), false)
    }

    private fun updateView(holder: ViewHolder, b: Boolean) {
        if(b) {
            holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.darkerGray))
            holder.timezone.setColorFilter(ContextCompat.getColor(context, R.color.darkerGray), PorterDuff.Mode.SRC_ATOP)
            holder.ringtone.setColorFilter(ContextCompat.getColor(context, R.color.darkerGray), PorterDuff.Mode.SRC_ATOP)
            holder.vibration.setColorFilter(ContextCompat.getColor(context, R.color.darkerGray), PorterDuff.Mode.SRC_ATOP)
            holder.snooze.setColorFilter(ContextCompat.getColor(context, R.color.darkerGray), PorterDuff.Mode.SRC_ATOP)
        }
        else {
            holder.amPm.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.lightGray))
            holder.timezone.setColorFilter(ContextCompat.getColor(context, R.color.lightGray), PorterDuff.Mode.SRC_ATOP)
            holder.ringtone.setColorFilter(ContextCompat.getColor(context, R.color.lightGray), PorterDuff.Mode.SRC_ATOP)
            holder.vibration.setColorFilter(ContextCompat.getColor(context, R.color.lightGray), PorterDuff.Mode.SRC_ATOP)
            holder.snooze.setColorFilter(ContextCompat.getColor(context, R.color.lightGray), PorterDuff.Mode.SRC_ATOP)
        }
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var mainView: ConstraintLayout = view.list_item_layout
        var amPm: TextView = view.am_pm
        var localTime: TextView = view.local_time
        var repeat: TextView = view.repeat
        var switch: Switch = view.on_off
        var colorTag: View = view.colorTag
        var range: TextView = view.range
        var timezone: ImageView = view.timezone
        var ringtone: ImageView = view.ringtone
        var vibration: ImageView = view.vibration
        var snooze: ImageView = view.snooze
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, item: AlarmItem)
        fun onItemStatusChanged(b: Boolean, item: AlarmItem)
    }
}