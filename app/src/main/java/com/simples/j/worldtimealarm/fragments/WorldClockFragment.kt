package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker

import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.support.ClockListAdapter
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.ListSwipeController
import kotlinx.android.synthetic.main.fragment_world_clock.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * A simple [Fragment] subclass.
 *
 */
class WorldClockFragment : Fragment(), View.OnClickListener, ListSwipeController.OnListControlListener {

    private lateinit var clockListAdapter: ClockListAdapter
    private lateinit var calendar: Calendar
    private lateinit var timeFormat: DateFormat
    private lateinit var dateFormat: DateFormat
    private lateinit var swipeHelper: ItemTouchHelper
    private lateinit var swipeController: ListSwipeController
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var fragmentLayout: CoordinatorLayout
    private var clockItems = ArrayList<ClockItem>()
    private var removedItem: ClockItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)
        fragmentLayout = view.findViewById(R.id.fragment_list)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        calendar = Calendar.getInstance()
        timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        timeFormat.timeZone = calendar.timeZone
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
        dateFormat.timeZone = calendar.timeZone

        world_time.text = timeFormat.format(calendar.time)
        world_date.text = dateFormat.format(calendar.time)

        time_zone.setOnClickListener(this)
        new_timezone.setOnClickListener(this)

        val timeDialog = TimePickerDialog(context!!, { _: TimePicker, hour: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
            timeFormat.timeZone = calendar.timeZone
            world_time.text = timeFormat.format(calendar.time)
            clockListAdapter.notifyDataSetChanged()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

        val dateDialog = DatePickerDialog(context!!, { _: DatePicker, year: Int, month: Int, day: Int ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
            dateFormat.timeZone = calendar.timeZone
            world_date.text = dateFormat.format(calendar.time)
            clockListAdapter.notifyDataSetChanged()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        world_time.setOnClickListener {
            timeDialog.show()
        }
        world_date.setOnClickListener {
            dateDialog.show()
        }


        time_zone.text = TimeZone.getDefault().id

        clockItems = DatabaseCursor(context!!).getClockList()
        clockListAdapter = ClockListAdapter(context!!, clockItems, calendar)
        recyclerLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
        clockList.layoutManager = recyclerLayoutManager
        clockList.adapter = clockListAdapter
        clockList.isNestedScrollingEnabled = false
        clock_content_layout.isNestedScrollingEnabled = false
        clockList.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))

        swipeController = ListSwipeController()
        swipeHelper = ItemTouchHelper(swipeController)
        swipeHelper.attachToRecyclerView(clockList)
        swipeController.setOnSwipeListener(this)
        setEmptyMessage()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    val formattedTimeZone = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID).replace(" ", "_")
                    calendar.timeZone = TimeZone.getTimeZone(formattedTimeZone)
                    time_zone.text = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)

                    calendar.set(Calendar.SECOND, 0)

                    timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
                    timeFormat.timeZone = calendar.timeZone
                    dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
                    dateFormat.timeZone = calendar.timeZone

                    world_time.text = timeFormat.format(calendar.time)
                    world_date.text = dateFormat.format(calendar.time)
                }
            }
            requestCode == TIME_ZONE_NEW_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    DatabaseCursor(context!!).insertClock(ClockItem(null, data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)))
                    clockItems.clear()
                    clockItems.addAll(DatabaseCursor(context!!).getClockList())
                }
            }
        }

        setEmptyMessage()
        clockListAdapter.notifyDataSetChanged()
    }

    override fun onClick(view: View?) {
        when(view!!.id) {
            R.id.time_zone -> {
                startActivityForResult(Intent(activity, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
            R.id.new_timezone -> {
                val intent = Intent(activity, TimeZoneSearchActivity::class.java)
                intent.putExtra(TIME_ZONE_NEW_KEY, TIME_ZONE_NEW_CODE.toString())
                startActivityForResult(intent, TIME_ZONE_NEW_CODE)
            }
        }
    }

    override fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val itemPosition = viewHolder.adapterPosition
        val previousPosition = recyclerLayoutManager.findFirstCompletelyVisibleItemPosition()

        removedItem = clockItems[itemPosition]
        clockListAdapter.removeItem(itemPosition)
        DatabaseCursor(context!!).removeClock(removedItem!!)
        setEmptyMessage()

        Snackbar.make(fragmentLayout, resources.getString(R.string.alarm_removed), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            DatabaseCursor(context!!).insertClock(removedItem!!)
            clockListAdapter.addItem(itemPosition, removedItem!!)
            recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
            setEmptyMessage()
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(clockItems, from, to)
        clockListAdapter.notifyItemMoved(from, to)
    }

    private fun setEmptyMessage() {
        if(clockItems.size < 1) {
            clockList.visibility = View.GONE
            clock_empty.visibility = View.VISIBLE
        }
        else {
            clockList.visibility = View.VISIBLE
            clock_empty.visibility = View.GONE
        }
    }

    companion object {
        private const val TIME_ZONE_REQUEST_CODE = 1
        private const val TIME_ZONE_NEW_CODE = 2
        const val TIME_ZONE_NEW_KEY = "REQUEST_KEY"
    }
}
