package com.simples.j.worldtimealarm.support

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.MediaCursor
import java.util.*
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.N)
class BaseTimeZonePickerAdapter<T : BaseTimeZonePickerAdapter.AdapterItem>(private val context: Context?, list: List<T>, showItemSummary: Boolean, headerText: String?, listener: OnListItemClickListener<T>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mOriginal: List<T> = list
    private var mList: List<T> = list
    private var mHeaderText: String? = headerText
    private var mShowHeader: Boolean = headerText != null
    private var mShowItemSummary: Boolean = showItemSummary
    private var mListener: OnListItemClickListener<T>? = listener

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

    override fun getItemCount(): Int = mList.size + getHeaderCount()

    override fun getItemId(position: Int): Long = if(isPositionHeader(position)) -1 else getData(position).itemId

    override fun getItemViewType(position: Int): Int = if(isPositionHeader(position)) TYPE_HEADER else TYPE_ITEM

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is HeaderViewHolder -> {
                val timeZone = mHeaderText.let {
                    if(it.isNullOrEmpty()) null
                    else android.icu.util.TimeZone.getTimeZone(it)
                }
                if(timeZone != null) {
                    val timeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), Date()).format(timeZone)

                    val name = MediaCursor.getBestNameForTimeZone(timeZone)

                    holder.title.text = name
                    holder.summary.text = timeZoneInfo.mGmtOffset
                }
            }
            is ItemViewHolder -> {
                val item = getData(position)
                holder.title.text = item.title
                if(mShowItemSummary) {
                    holder.summaryLayout.visibility = View.VISIBLE
                    holder.summary.text = item.summary

                    val difference = TimeZone.getTimeZone(item.id).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())

                    holder.difference.text = MediaCursor.getOffsetOfDifference(context!!, difference, MediaCursor.TYPE_CURRENT)
                }
                else holder.summaryLayout.visibility = View.GONE

                holder.itemView.setOnClickListener {
                    mListener?.onListItemClick(item)
                }
            }
        }
    }

    private fun getData(position: Int): T {
        return mList[position - getHeaderCount()]
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
            list = ArrayList<T>()
            val prefix = text.toLowerCase(locale)
            mOriginal.forEach {
                for(key in it.searchKeys) {
                    val lower = key.toLowerCase(locale)
                    if(lower.contains(text)) {
                        (list as ArrayList<T>).add(it)
                        return@forEach
                    }
                }
            }
        }
        mList = list
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