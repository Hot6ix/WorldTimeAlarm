package com.simples.j.worldtimealarm.support

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import kotlinx.android.synthetic.main.content_selector_item.view.*
import java.lang.Exception

class ContentSelectorAdapter(val context: Context, val array: ArrayList<out Any>, selected: Any? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listener: OnItemSelectedListener? = null
    private var lastSelected: Int = array.indexOf(selected)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType){
            TYPE_ITEM, TYPE_USER_RINGTONE, TYPE_SYSTEM_RINGTONE -> {
                ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.content_selector_item, parent, false))
            }
            TYPE_CATEGORY -> {
                CategoryViewHolder(LayoutInflater.from(context).inflate(R.layout.content_selector_category, parent, false))
            }
            else -> {
                ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.content_selector_item, parent, false))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = array[position]
        return if(item is RingtoneItem) {
            try {
                if(item.uri == "user:ringtone"  || item.uri == "system:ringtone") {
                    TYPE_CATEGORY
                }
                else {
                    val uri = Uri.parse(item.uri)
                    uri.path?.let {
                        if(it.startsWith("/internal")) TYPE_SYSTEM_RINGTONE
                        else TYPE_USER_RINGTONE
                    } ?: TYPE_SYSTEM_RINGTONE
                }
            } catch (e: Exception) {
                TYPE_SYSTEM_RINGTONE
            }
        }
        else TYPE_ITEM
    }

    override fun getItemCount(): Int = array.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = array[holder.adapterPosition]) {
            is RingtoneItem -> {
                when(holder.itemViewType) {
                    TYPE_CATEGORY -> {
                        (holder as CategoryViewHolder).title.text = item.title
                    }
                    TYPE_SYSTEM_RINGTONE, TYPE_USER_RINGTONE -> {
                        (holder as ItemViewHolder).title.text = item.title
                        holder.selector.isChecked = lastSelected == holder.adapterPosition
                    }
                }
            }
            is PatternItem -> {
                (holder as ItemViewHolder).title.text = item.title
                holder.selector.isChecked = lastSelected == holder.adapterPosition
            }
            is SnoozeItem -> {
                (holder as ItemViewHolder).title.text = item.title
                holder.selector.isChecked = lastSelected == holder.adapterPosition
            }
        }
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        this.listener = listener
    }

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val selector: RadioButton = view.selector
        val title: TextView = view.title

        init {
            view.setOnClickListener {
                lastSelected = adapterPosition
                notifyItemRangeChanged(0, array.size - 1)
                listener?.onItemSelected(adapterPosition, array[adapterPosition])
            }
        }
    }

    inner class CategoryViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView = view.title
    }

    interface OnItemSelectedListener {
        fun onItemSelected(index: Int, item: Any)
    }

    companion object {
        const val TYPE_CATEGORY = 1
        const val TYPE_USER_RINGTONE = 10
        const val TYPE_SYSTEM_RINGTONE = 20
        const val TYPE_ITEM = 30
    }

}