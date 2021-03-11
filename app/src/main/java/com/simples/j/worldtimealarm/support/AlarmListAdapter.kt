package com.simples.j.worldtimealarm.support

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.AlarmStatus
import com.simples.j.worldtimealarm.etc.AlarmWarningReason
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.AlarmStringFormatHelper
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.alarm_list_item.view.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

/**
 * Created by j on 19/02/2018.
 *
 */
class AlarmListAdapter(private var list: ArrayList<AlarmItem>, private val context: Context): RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {

    private lateinit var listener: OnItemClickListener
    private var highlightId: Int = -1
    private var warningList: List<Pair<String, AlarmWarningReason>>? = null

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

    // TODO: Replace deprecated
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

        val expect = try {
            AlarmController.getInstance().calculateDateTime(item, AlarmController.TYPE_ALARM)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val mainZonedDateTime = expect?.withZoneSameInstant(ZoneId.systemDefault())

        val startDate =
                item.startDate.let {
                    if(it != null && it > 0) {
                        val startInstant = Instant.ofEpochMilli(it)
                        ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault())
                    }
                    else null
                }

        val endDate =
                item.endDate.let {
                    if(it != null && it > 0) {
                        val endInstant = Instant.ofEpochMilli(it)
                        ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault())
                    }
                    else null
                }

        val isExpired = item.isExpired(expect)
        if(startDate == null && endDate == null) {
            holder.range.visibility = View.GONE
            holder.switch.isEnabled = true
        }
        else {
            holder.switch.isEnabled = !isExpired

            val rangeText = AlarmStringFormatHelper.getDisplayLocalDate(context, startDate, endDate, item.hasRepeatDay())

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

        mainZonedDateTime?.let {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, it.hour)
                set(Calendar.MINUTE, it.minute)
            }
            holder.localTime.text = DateFormat.format(MediaCursor.getLocalizedTimeFormat(), calendar)
        }

        with(item.repeat) {
            if (this.any { it > 0 }) {
                holder.repeat.text = AlarmStringFormatHelper.getDisplayLocalRepeatArray(context, item.repeat, mainZonedDateTime, item.timeZone)
            } else {
                mainZonedDateTime?.toInstant()?.let {
                    holder.repeat.text =
                            when {
                                DateUtils.isToday(it.toEpochMilli()) && it.isAfter(Instant.now()) && endDate == null -> {
                                    context.resources.getString(R.string.today)
                                }
                                (it.isBefore(Instant.now()) || DateUtils.isToday(it.toEpochMilli() - DateUtils.DAY_IN_MILLIS)) && startDate == null && endDate == null -> {
                                    context.resources.getString(R.string.tomorrow)
                                } // this can make adapter to know calendar date is tomorrow
                                endDate != null -> {
                                    context.resources.getString(R.string.everyday)
                                }
                                startDate != null -> {
                                    DateUtils.formatDateTime(context, startDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                                }
                                else -> {
                                    DateUtils.formatDateTime(context, it.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                                }
                            }
                }
            }
        }

        item.label.let {
            if(it.isNullOrEmpty()) holder.label.visibility = View.GONE
            else {
                holder.label.apply {
                    text = item.label
                    visibility = View.VISIBLE
                }
            }
        }

        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.on_off != 0

        if(warningList?.any { it.first.toIntOrNull() == item.id } == true) {
            holder.warning.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { listener.onItemClicked(it, item, AlarmStatus.STATUS_V22_UPDATE_ERROR) }
        }
        else {
            holder.warning.visibility = View.GONE
            holder.itemView.setOnClickListener { listener.onItemClicked(it, item) }
        }

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
            else {
                holder.vibration.visibility = View.GONE
            }
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

    fun setWarningMark(list: List<Pair<String, AlarmWarningReason>>?) {
        warningList = list
        notifyItemRangeChanged(0, itemCount)
    }

    fun setOnItemListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setHighlightId(id: Int) {
        this.highlightId = id
    }

    private fun updateView(holder: ViewHolder, b: Boolean) {
        if(b) {
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.label.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.timezone.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.ringtone.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.vibration.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.snooze.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
        }
        else {
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.label.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.timezone.setColorFilter(ContextCompat.getColor(context, R.color.textColorDisabled), PorterDuff.Mode.SRC_ATOP)
            holder.ringtone.setColorFilter(ContextCompat.getColor(context, R.color.textColorDisabled), PorterDuff.Mode.SRC_ATOP)
            holder.vibration.setColorFilter(ContextCompat.getColor(context, R.color.textColorDisabled), PorterDuff.Mode.SRC_ATOP)
            holder.snooze.setColorFilter(ContextCompat.getColor(context, R.color.textColorDisabled), PorterDuff.Mode.SRC_ATOP)
        }
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var mainView: ConstraintLayout = view.list_item_layout
        var localTime: TextView = view.local_time
        var repeat: TextView = view.repeat
        var switch: SwitchCompat = view.on_off
        var colorTag: View = view.colorTag
        var range: TextView = view.range
        var label: TextView = view.label
        var warning: ImageView = view.warning
        var timezone: ImageView = view.timezone
        var ringtone: ImageView = view.ringtone
        var vibration: ImageView = view.vibration
        var snooze: ImageView = view.snooze
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, item: AlarmItem, status: AlarmStatus = AlarmStatus.STATUS_NORMAL)
        fun onItemStatusChanged(b: Boolean, item: AlarmItem)
    }
}