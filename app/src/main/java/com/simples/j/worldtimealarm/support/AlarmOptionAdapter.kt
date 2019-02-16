package com.simples.j.worldtimealarm.support

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.OptionItem
import kotlinx.android.synthetic.main.alarm_option_item.view.*

/**
 * Created by j on 20/02/2018.
 *
 */
class AlarmOptionAdapter(private val options: ArrayList<OptionItem>, private var context: Context): RecyclerView.Adapter<AlarmOptionAdapter.ViewHolder>() {

    private lateinit var onItemClickListener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.alarm_option_item, parent, false))
    }

    override fun getItemCount() = options.size

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = position

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = options[position].title
        holder.summary.text = options[position].summary
        if(position == options.lastIndex) {
            if(options[position].summary == "0") {
                holder.summary.text = context.resources.getString(R.string.not_set)
            }
            else {
                holder.summary.visibility = View.GONE
                holder.colorTag.visibility = View.VISIBLE
                holder.colorTag.setBackgroundColor(options[position].summary.toInt())
            }
        }
        holder.itemView.isClickable = true
        holder.itemView.setOnClickListener { onItemClickListener.onItemClick(it, position, options[position]) }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var title: TextView = view.option_title
        var summary: TextView = view.option_summary
        var colorTag: View = view.option_colorTag
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, item: OptionItem)
    }
}