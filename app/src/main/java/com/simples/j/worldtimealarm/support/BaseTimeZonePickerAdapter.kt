package com.simples.j.worldtimealarm.support

import android.content.Context
import android.icu.text.LocaleDisplayNames
import android.icu.util.TimeZone
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.utils.MediaCursor
import java.util.*
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.N)
class BaseTimeZonePickerAdapter<T : BaseTimeZonePickerAdapter.AdapterItem>(private val context: Context?,
                                                                           private var list: List<T>,
                                                                           private val showItemSummary: Boolean,
                                                                           private val showItemDifference: Boolean,
                                                                           private var headerText: String?,
                                                                           private var listener: OnListItemClickListener<T>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mOriginal: List<T> = list
    private var mShowHeader: Boolean = headerText != null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(type) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.time_zone_picker_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = inflater.inflate(R.layout.time_zone_picker_item, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalStateException("Unexpected viewType : $type")
        }
    }

    override fun getItemCount(): Int = list.size + getHeaderCount()

    override fun getItemId(position: Int): Long = if(isPositionHeader(position)) -1 else getData(position).itemId

    override fun getItemViewType(position: Int): Int = if(isPositionHeader(position)) TYPE_HEADER else TYPE_ITEM

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is HeaderViewHolder -> {
                holder.title.text = LocaleDisplayNames.getInstance(Locale.getDefault()).regionDisplayName(headerText)
                holder.summary.visibility = View.GONE
            }
            is ItemViewHolder -> {
                val item = getData(position)
                holder.title.text = item.title
                if(showItemSummary) {
                    holder.summaryLayout.visibility = View.VISIBLE
                    holder.summary.text = item.summary

                    if(showItemDifference) {
                        val difference = TimeZone.getTimeZone(item.id).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
                        holder.difference.visibility = View.VISIBLE
                        holder.difference.text = MediaCursor.getOffsetOfDifference(context!!, difference, MediaCursor.TYPE_CONVERTER)
                    }
                    else {
                        holder.difference.visibility = View.GONE
                    }
                }
                else holder.summaryLayout.visibility = View.GONE

                holder.itemView.setOnClickListener {
                    listener?.onListItemClick(item)
                }
            }
        }
    }

    private fun getData(position: Int): T {
        return list[position - getHeaderCount()]
    }

    private class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.time_zone_picker_title)
        val summaryLayout: ConstraintLayout = view.findViewById(R.id.time_zone_picker_summary_layout)
        val summary: TextView = view.findViewById(R.id.time_zone_picker_summary)
        val difference: TextView = view.findViewById(R.id.time_zone_picker_difference)
    }

    private class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.time_zone_picker_header_title)
        val summary: TextView = view.findViewById(R.id.time_zone_picker_header_summary)
    }

    private fun getHeaderCount(): Int = if(mShowHeader) 1 else 0

    private fun isPositionHeader(position: Int): Boolean = mShowHeader && position == 0

    fun filterByText(text: String) {
        val list: List<T>
        val locale = Locale.getDefault()
        if(TextUtils.isEmpty(text)) {
            list = mOriginal
        }
        else {
            list = ArrayList()
            val prefix = text.toLowerCase(locale)
            mOriginal.forEach {
                for(key in it.searchKeys) {
                    val lower = key.toLowerCase(locale)
                    if(lower.contains(prefix)) {
                        list.add(it)
                        return@forEach
                    }
                }
            }
        }
        this.list = list
        notifyDataSetChanged()
    }

    interface AdapterItem {
        val id: String
        val title: String
        val summary: String?
        val itemId: Long
        val searchKeys: Array<String>
    }

    interface OnListItemClickListener<T> {
        fun onListItemClick(item: T)
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }
}