package com.simples.j.worldtimealarm.support

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import kotlinx.android.synthetic.main.content_selector_item.view.*

class ContentSelectorAdapter(val context: Context, val array: ArrayList<out Any>, selected: Any? = null, private val defaultRingtoneItem: RingtoneItem? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemSelectedListener: OnItemSelectedListener? = null
    private var onItemMenuSelectedListener: OnItemMenuSelectedListener? = null
    private var lastSelected = selected

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType){
            TYPE_ADD, TYPE_ITEM, TYPE_USER_RINGTONE, TYPE_SYSTEM_RINGTONE -> {
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
                when(item.uri) {
                    "user:ringtone", "system:ringtone" -> {
                        TYPE_CATEGORY
                    }
                    "add:ringtone" -> {
                        TYPE_ADD
                    }
                    else -> {
                        val uri = Uri.parse(item.uri)
                        uri.path?.let {
                            if(it.startsWith("/internal")) TYPE_SYSTEM_RINGTONE
                            else TYPE_USER_RINGTONE
                        } ?: TYPE_SYSTEM_RINGTONE
                    }
                }
            } catch (e: Exception) {
                TYPE_SYSTEM_RINGTONE
            }
        }
        else TYPE_ITEM
    }

    override fun getItemCount(): Int = array.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = array[holder.adapterPosition]
        when(item) {
            is RingtoneItem -> {
                when(holder.itemViewType) {
                    TYPE_CATEGORY -> {
                        (holder as CategoryViewHolder).title.text = item.title
                    }
                    TYPE_ADD -> {
                        (holder as ItemViewHolder).title.text = item.title
                        holder.icon.setImageResource(R.drawable.ic_action_add)
                    }
                    TYPE_SYSTEM_RINGTONE, TYPE_USER_RINGTONE -> {
                        (holder as ItemViewHolder).title.text = item.title

                        holder.selector.visibility =
                                if(array.indexOf(lastSelected) == holder.adapterPosition && holder.itemViewType != TYPE_ADD) View.VISIBLE
                                else View.INVISIBLE

                        holder.icon.setImageResource(R.drawable.ic_ringtone_white)

                        if(item.uri.isEmpty() || item.uri == "null") holder.icon.imageTintMode = PorterDuff.Mode.CLEAR
                        else holder.icon.imageTintMode = PorterDuff.Mode.SRC_ATOP

                        if(holder.itemViewType == TYPE_USER_RINGTONE) {
                            holder.itemView.setOnLongClickListener {

                                val menu = PopupMenu(context, holder.selector)
                                menu.inflate(R.menu.menu_ringtone_item)
                                menu.setOnMenuItemClickListener {

                                    onItemMenuSelectedListener?.onItemMenuSelected(position, it, TYPE_USER_RINGTONE, item)
                                    if(it.itemId == R.id.action_remove) {
                                        // set to default ringtone if deleting item and selected item are same
                                        if(lastSelected == item) {
                                            defaultRingtoneItem?.let { default ->
                                                lastSelected = default
                                                onItemSelectedListener?.onItemSelected(array.indexOf(default), default, ACTION_NOT_PLAY)
                                            }
                                        }
                                        array.removeAt(holder.adapterPosition)
                                        notifyItemRemoved(holder.adapterPosition)
                                        notifyItemRangeChanged(holder.adapterPosition, itemCount)
                                    }
                                    true
                                }
                                menu.show()
                                true
                            }
                        }

                    }
                }
            }
            is PatternItem -> {
                (holder as ItemViewHolder).title.text = item.title

                holder.icon.setImageResource(R.drawable.ic_vibration_white)

                if(item.array == null) holder.icon.imageTintMode = PorterDuff.Mode.CLEAR
                else holder.icon.imageTintMode = PorterDuff.Mode.SRC_ATOP

                holder.selector.visibility =
                        if(array.indexOf(lastSelected) == position) View.VISIBLE
                        else View.INVISIBLE
            }
            is SnoozeItem -> {
                (holder as ItemViewHolder).title.text = item.title

                holder.icon.setImageResource(R.drawable.ic_snooze_white)

                if(item.duration == 0L) holder.icon.imageTintMode = PorterDuff.Mode.CLEAR
                else holder.icon.imageTintMode = PorterDuff.Mode.SRC_ATOP

                holder.selector.visibility =
                        if(array.indexOf(lastSelected) == position) View.VISIBLE
                        else View.INVISIBLE
            }
        }

        if(holder.itemViewType != TYPE_CATEGORY) {
            holder.itemView.setOnClickListener {
                if(holder.itemViewType != TYPE_ADD) {
                    lastSelected = item
                    notifyItemRangeChanged(0, array.size)
                }

                onItemSelectedListener?.onItemSelected(holder.adapterPosition, item)
            }
        }
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        this.onItemSelectedListener = listener
    }

    fun setOnItemMenuSelectedListener(listener: OnItemMenuSelectedListener) {
        this.onItemMenuSelectedListener = listener
    }

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.icon
        val title: TextView = view.title
        val selector: ImageView = view.selector
    }

    inner class CategoryViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView = view.title
    }

    interface OnItemSelectedListener {
        fun onItemSelected(index: Int, item: Any, action: Int? = null)
    }

    interface OnItemMenuSelectedListener {
        fun onItemMenuSelected(index: Int, menu: MenuItem, type: Int, item: Any)
    }

    companion object {
        const val TYPE_CATEGORY = 1
        const val TYPE_ADD = 2
        const val TYPE_USER_RINGTONE = 10
        const val TYPE_SYSTEM_RINGTONE = 20
        const val TYPE_ITEM = 30

        const val URI_USER_RINGTONE = "user:ringtone"
        const val URI_SYSTEM_RINGTONE = "system:ringtone"
        const val URI_ADD_RINGTONE = "add:ringtone"

        const val ACTION_PLAY = 100
        const val ACTION_NOT_PLAY = 200
    }

}