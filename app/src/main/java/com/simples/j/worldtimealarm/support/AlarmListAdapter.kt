package com.simples.j.worldtimealarm.support

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.utils.AlarmController
import kotlinx.android.synthetic.main.alarm_list_item.view.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

/**
 * Created by j on 19/02/2018.
 *
 */
class AlarmListAdapter(private var list: ArrayList<AlarmItem>, private val context: Context): RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {

    private lateinit var listener: OnItemClickListener
    private var highlightId: Int = -1

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

        val instant = Instant.ofEpochMilli(item.timeSet.toLong())
        val mainZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())

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

        if(startDate == null && endDate == null) {
            holder.range.visibility = View.GONE
            holder.switch.isEnabled = true
        }
        else {
            // alarm is one-time and start date is set
            // if start date passed, disable alarm
            holder.switch.isEnabled = startDate.let { date ->
                if(date != null && !item.repeat.any { it > 0 }) {
                    date.isAfter(ZonedDateTime.now())
                }
                else true
            }

            // disable switch if alarm is expired
            endDate?.let {
                val expect = try {
                    AlarmController.getInstance().calculateDateTime(item, AlarmController.TYPE_ALARM)
                } catch (e: IllegalStateException) {
                    null
                }

                holder.switch.isEnabled =
                        if(expect == null) false
                        else expect.isBefore(it) || expect.isEqual(it)
            }

            val rangeText = when {
                startDate != null && endDate != null -> {
                    DateUtils.formatDateRange(context, startDate.toInstant().toEpochMilli(), endDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
                }
                startDate != null -> {
                    if(item.repeat.any { it > 0 }) context.getString(R.string.range_begin).format(DateUtils.formatDateTime(context, startDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                    else null
                }
                endDate != null -> {
                    context.getString(R.string.range_until).format(DateUtils.formatDateTime(context, endDate.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
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

        holder.localTime.text = mainZonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

        with(item.repeat) {
            if(this.any { it > 0 }) {
                val difference = TimeZone.getTimeZone(item.timeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
                val itemCalendar = Calendar.getInstance().apply {
                    timeInMillis = item.timeSet.toLong()
                }
                val tmp = itemCalendar.clone() as Calendar
                tmp.add(Calendar.MILLISECOND, difference)

                // for support old version of app
                val repeat = item.repeat.mapIndexed { index, i ->
                    if(i > 0) index + 1 else 0
                }.filter { it != 0 }.map {
                    var converted = it - 1
                    if(converted == 0) converted = 7

                    DayOfWeek.of(converted)
                }

                val repeatLocal = repeat.map {
                    val targetZonedDateTime = mainZonedDateTime
                            .withZoneSameInstant(ZoneId.of(item.timeZone))
                            .with(TemporalAdjusters.nextOrSame(it))

                    targetZonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).dayOfWeek
                }

                holder.repeat.text =
                        if(repeatLocal.size == 7) context.resources.getString(R.string.everyday)
                        else if(repeatLocal.contains(DayOfWeek.SATURDAY) && repeatLocal.contains(DayOfWeek.SUNDAY) && repeatLocal.size == 2) context.resources.getString(R.string.weekend)
                        else if(repeatLocal.contains(DayOfWeek.MONDAY)
                                && repeatLocal.contains(DayOfWeek.TUESDAY)
                                && repeatLocal.contains(DayOfWeek.WEDNESDAY)
                                && repeatLocal.contains(DayOfWeek.THURSDAY)
                                && repeatLocal.contains(DayOfWeek.FRIDAY)
                                && repeatLocal.size == 5) context.resources.getString(R.string.weekday)
                        else if(repeatLocal.size == 1) {
                            repeatLocal[0].getDisplayName(TextStyle.FULL, Locale.getDefault())
                        }
                        else {
                            repeatLocal.joinToString {
                                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            }
                        }
            }
            else {
                val mainInstant = mainZonedDateTime.toInstant()
                holder.repeat.text =
                        when {
                            DateUtils.isToday(mainInstant.toEpochMilli()) && mainInstant.isAfter(Instant.now()) && endDate == null -> {
                                context.resources.getString(R.string.today)
                            }
                            (mainInstant.isBefore(Instant.now()) || DateUtils.isToday(mainInstant.toEpochMilli() - DateUtils.DAY_IN_MILLIS)) && startDate == null && endDate == null -> {
                                context.resources.getString(R.string.tomorrow)
                            } // this can make adapter to know calendar date is tomorrow
                            endDate != null -> {
                                context.resources.getString(R.string.everyday)
                            }
                            startDate != null -> {
                                DateUtils.formatDateTime(context, mainInstant.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                            }
                            else -> {
                                DateUtils.formatDateTime(context, mainInstant.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                            }
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

    private fun updateView(holder: ViewHolder, b: Boolean) {
        if(b) {
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.textColorEnabled))
            holder.timezone.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.ringtone.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.vibration.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
            holder.snooze.setColorFilter(ContextCompat.getColor(context, R.color.textColorEnabled), PorterDuff.Mode.SRC_ATOP)
        }
        else {
            holder.localTime.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.repeat.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
            holder.range.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
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