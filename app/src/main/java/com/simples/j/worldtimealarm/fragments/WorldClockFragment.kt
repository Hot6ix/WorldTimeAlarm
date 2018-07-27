package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.preference.PreferenceManager
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
import com.simples.j.worldtimealarm.TimeSetChangedReceiver
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_NEW_CODE
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.support.ClockListAdapter
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.ListSwipeController
import kotlinx.android.synthetic.main.fragment_world_clock.*
import java.text.DateFormat
import java.util.*

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
    private lateinit var timeZoneChangedReceiver: UpdateRequestReceiver
    private lateinit var timeZone: TimeZone
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
        val converterTimezoneId = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "")
        timeZone =
                if(converterTimezoneId.isEmpty()) calendar.timeZone
                else TimeZone.getTimeZone(converterTimezoneId)
        calendar.timeZone = timeZone
        timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        timeFormat.timeZone = timeZone
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
        dateFormat.timeZone = timeZone

        world_time.text = timeFormat.format(calendar.time)
        world_date.text = dateFormat.format(calendar.time)

        time_zone.setOnClickListener(this)
        new_timezone.setOnClickListener(this)

        val timeDialog = TimePickerDialog(context!!, { _: TimePicker, hour: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
            timeFormat.timeZone = timeZone
            world_time.text = timeFormat.format(calendar.time)
            clockListAdapter.notifyDataSetChanged()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

        val dateDialog = DatePickerDialog(context!!, { _: DatePicker, year: Int, month: Int, day: Int ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
            dateFormat.timeZone = timeZone
            world_date.text = dateFormat.format(calendar.time)
            clockListAdapter.notifyDataSetChanged()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        world_time.setOnClickListener {
            timeDialog.show()
        }
        world_date.setOnClickListener {
            dateDialog.show()
        }


        time_zone.text = timeZone.id.replace("_", " ")

        clockItems = DatabaseCursor(context!!).getClockList()
        clockListAdapter = ClockListAdapter(context!!, clockItems, calendar)
        recyclerLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
        clockList.layoutManager = recyclerLayoutManager
        clockList.adapter = clockListAdapter
        clockList.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))

        swipeController = ListSwipeController()
        swipeHelper = ItemTouchHelper(swipeController)
        swipeHelper.attachToRecyclerView(clockList)
        swipeController.setOnSwipeListener(this)

        timeZoneChangedReceiver = UpdateRequestReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_TIME_ZONE_CHANGED)
        context!!.registerReceiver(timeZoneChangedReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        context!!.unregisterReceiver(timeZoneChangedReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    timeZone = TimeZone.getTimeZone(data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID))
                    updateStandardTimeZone()
                }
            }
            requestCode == TIME_ZONE_NEW_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    DatabaseCursor(context!!).insertClock(ClockItem(null, data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID), -1))
                    clockItems.clear()
                    clockItems.addAll(DatabaseCursor(context!!).getClockList())
                }
            }
        }

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

        Snackbar.make(fragmentLayout, resources.getString(R.string.alarm_removed), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            val id = DatabaseCursor(context!!).insertClock(removedItem!!)
            removedItem!!.id = id.toInt()
            clockListAdapter.addItem(itemPosition, removedItem!!)
            recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(clockItems, from, to)
        clockListAdapter.notifyItemMoved(from, to)
        DatabaseCursor(context!!).swapClockOrder(clockItems[from], clockItems[to])
        val tmp = clockItems[from].index
        clockItems[from].index = clockItems[to].index
        clockItems[to].index = tmp
    }

    private fun updateStandardTimeZone() {
        val formattedTimeZone = timeZone.id.replace("_", " ")
        calendar.timeZone = timeZone
        time_zone.text = formattedTimeZone

        calendar.set(Calendar.SECOND, 0)

        timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        timeFormat.timeZone = timeZone
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
        dateFormat.timeZone = timeZone

        world_time.text = timeFormat.format(calendar.time)
        world_date.text = dateFormat.format(calendar.time)
    }


    inner class UpdateRequestReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(C.TAG, intent.action)
            when(intent.action) {
                ACTION_TIME_ZONE_CHANGED -> {
                    timeZone = TimeZone.getTimeZone(intent.getStringExtra(TIME_ZONE_CHANGED_KEY))
                    updateStandardTimeZone()
                    clockListAdapter.notifyDataSetChanged()
                }
            }
        }

    }

    companion object {
        const val TIME_ZONE_NEW_KEY = "REQUEST_KEY"
        const val TIME_ZONE_CHANGED_KEY = "TIME_ZONE_ID"
        const val ACTION_TIME_ZONE_CHANGED = "com.simples.j.worldtimealarm.APP_TIMEZONE_CHANGED"
    }
}
