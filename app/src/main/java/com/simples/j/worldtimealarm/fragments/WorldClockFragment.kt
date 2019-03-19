package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.*
import android.os.Build
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
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_NEW_CODE
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.support.ClockListAdapter
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.ListSwipeController
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_world_clock.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * A simple [Fragment] subclass.
 *
 */
class WorldClockFragment : Fragment(), View.OnClickListener, ListSwipeController.OnListControlListener, CoroutineScope, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private lateinit var clockListAdapter: ClockListAdapter
    private lateinit var swipeHelper: ItemTouchHelper
    private lateinit var swipeController: ListSwipeController
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var fragmentLayout: CoordinatorLayout
    private lateinit var timeZoneChangedReceiver: UpdateRequestReceiver
    private lateinit var timeZone: TimeZone
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var timeDialog: TimePickerDialogFragment
    private lateinit var dateDialog: DatePickerDialogFragment

    private var timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
    private var dateFormat = DateFormat.getDateInstance(DateFormat.FULL)
    private var calendar = Calendar.getInstance()
    private var clockItems = ArrayList<ClockItem>()
    private var removedItem: ClockItem? = null

    private lateinit var mPrefManager: SharedPreferences
    private var mTimeZoneSelectorOption: String = ""

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)
        fragmentLayout = view.findViewById(R.id.fragment_list)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dbCursor = DatabaseCursor(context!!)

        mPrefManager = PreferenceManager.getDefaultSharedPreferences(context)
        val isUserTimeZoneEnabled = mPrefManager.getBoolean(resources.getString(R.string.setting_converter_timezone_key), false)
        val converterTimezoneId = mPrefManager.getString(resources.getString(R.string.setting_converter_timezone_id_key), TimeZone.getDefault().id)
        mTimeZoneSelectorOption = mPrefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD

        timeZone =
                if(savedInstanceState != null && !savedInstanceState.isEmpty) TimeZone.getTimeZone(savedInstanceState.getString(USER_SELECTED_TIMEZONE))
                else if(!isUserTimeZoneEnabled) calendar.timeZone
                else TimeZone.getTimeZone(converterTimezoneId)
        calendar.timeZone = timeZone
        timeFormat.timeZone = timeZone
        dateFormat.timeZone = timeZone

        if(savedInstanceState != null && !savedInstanceState.isEmpty) {
            calendar.timeInMillis = savedInstanceState.getLong(USER_SELECTED_DATE_AND_TIME)
        }

        world_am_pm.text = if(calendar.get(Calendar.AM_PM) == 0) context!!.getString(R.string.am) else context!!.getString(R.string.pm)
        world_time.text = timeFormat.format(calendar.time)
        world_date.text = dateFormat.format(calendar.time)

        time_zone.setOnClickListener(this)
        new_timezone.setOnClickListener(this)

        timeDialog =
                fragmentManager?.findFragmentByTag(TAG_FRAGMENT_TIME_DIALOG) as? TimePickerDialogFragment ?:
                TimePickerDialogFragment.newInstance().apply {
                    setTime(this@WorldClockFragment.calendar)
                }
        timeDialog.setTimeSetListener(this)

        dateDialog =
                fragmentManager?.findFragmentByTag(TAG_FRAGMENT_DATE_DIALOG) as? DatePickerDialogFragment ?:
                DatePickerDialogFragment.newInstance().apply {
                    setDate(this@WorldClockFragment.calendar)
                    minDate = 0
                }
        dateDialog.setDateSetListener(this)

        world_time_layout.setOnClickListener {
            if(!timeDialog.isAdded) timeDialog.show(fragmentManager, TAG_FRAGMENT_TIME_DIALOG)
        }
        world_date.setOnClickListener {
            if(!dateDialog.isAdded) dateDialog.show(fragmentManager, TAG_FRAGMENT_DATE_DIALOG)
        }

        time_zone.text = getNameForTimeZone(timeZone.id)

        job = launch(coroutineContext) {
            withContext(Dispatchers.IO) {
                clockItems = dbCursor.getClockList()
            }

            if(context != null) {
                clockListAdapter = ClockListAdapter(context!!, clockItems, calendar)
                recyclerLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
                clockList.layoutManager = recyclerLayoutManager
                clockList.adapter = clockListAdapter
                clockList.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))

                swipeController = ListSwipeController()
                swipeController.setOnSwipeListener(this@WorldClockFragment)

                swipeHelper = ItemTouchHelper(swipeController)
                swipeHelper.attachToRecyclerView(clockList)
                setEmptyMessage()

                timeZoneChangedReceiver = UpdateRequestReceiver()
                val intentFilter = IntentFilter().apply {
                    addAction(ACTION_TIME_ZONE_CHANGED)
                    addAction(ACTION_TIME_ZONE_SELECTOR_CHANGED)
                }
                context?.registerReceiver(timeZoneChangedReceiver, intentFilter)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if(::timeZoneChangedReceiver.isInitialized)
            context?.unregisterReceiver(timeZoneChangedReceiver)
        else {
            launch(coroutineContext) {
                job.cancelAndJoin()

                context?.unregisterReceiver(timeZoneChangedReceiver)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(USER_SELECTED_TIMEZONE, timeZone.id)
        outState.putLong(USER_SELECTED_DATE_AND_TIME, calendar.timeInMillis)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    timeZone = TimeZone.getTimeZone(data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID).replace(" ", "_"))
                    updateStandardTimeZone()
                }
            }
            requestCode == TIME_ZONE_NEW_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    DatabaseCursor(context!!).insertClock(ClockItem(null, data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID), -1))
                    clockItems.clear()
                    clockItems.addAll(DatabaseCursor(context!!).getClockList())
                    setEmptyMessage()
                }
            }
        }

        if(::clockListAdapter.isInitialized) {
            clockListAdapter.notifyItemRangeChanged(0, clockItems.count())
            if(clockItems.isNotEmpty())
                clockList.smoothScrollToPosition(clockItems.count() - 1)
        }
        else {
            launch(coroutineContext) {
                job.join()
                clockListAdapter.notifyItemRangeChanged(0, clockItems.count())
                clockList.smoothScrollToPosition(clockItems.count() - 1)
            }
        }
    }

    override fun onClick(view: View?) {
        when(view!!.id) {
            R.id.time_zone -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(context, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_CHANGE)
                        putExtra(TimeZonePickerActivity.TIME_ZONE_ID, timeZone.id)
                        putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_WORLD_CLOCK)
                    }
                    startActivityForResult(i, TIME_ZONE_REQUEST_CODE)
                }
                else startActivityForResult(Intent(activity, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
            R.id.new_timezone -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(context, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_ADD)
                        putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_WORLD_CLOCK)
                    }
                    startActivityForResult(i, TIME_ZONE_NEW_CODE)
                }
                else {
                    val intent = Intent(activity, TimeZoneSearchActivity::class.java)
                    intent.putExtra(TIME_ZONE_NEW_KEY, TIME_ZONE_NEW_CODE.toString())
                    startActivityForResult(intent, TIME_ZONE_NEW_CODE)
                }
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
            val id = DatabaseCursor(context!!).insertClock(removedItem!!)
            removedItem!!.id = id.toInt()
            clockListAdapter.addItem(itemPosition, removedItem!!)
            recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
            setEmptyMessage()
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

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        timeDialog.setTime(calendar)
        world_time.text = timeFormat.format(calendar.time)
        world_am_pm.text = if(calendar.get(Calendar.AM_PM) == 0) context!!.getString(R.string.am) else context!!.getString(R.string.pm)
        clockListAdapter.notifyItemRangeChanged(0, clockItems.count())
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        dateDialog.setDate(calendar)
        world_date.text = dateFormat.format(calendar.time)
        clockListAdapter.notifyItemRangeChanged( 0, clockItems.count())
    }

    private fun updateStandardTimeZone() {
        time_zone.text = getNameForTimeZone(timeZone.id)

        calendar.timeZone = timeZone
        timeFormat.timeZone = timeZone
        dateFormat.timeZone = timeZone

        calendar.set(Calendar.SECOND, 0)

        world_am_pm.text = if(calendar.get(Calendar.AM_PM) == 0) context!!.getString(R.string.am) else context!!.getString(R.string.pm)
        world_time.text = timeFormat.format(calendar.time)
        world_date.text = dateFormat.format(calendar.time)
    }

    private fun setEmptyMessage() {
        if(clockItems.size < 1) {
            clockList.visibility = View.GONE
            list_empty.visibility = View.VISIBLE
        }
        else {
            clockList.visibility = View.VISIBLE
            list_empty.visibility = View.GONE
        }
    }

    private fun getNameForTimeZone(timeZoneId: String?): String {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(timeZoneId))
        }
        else timeZoneId ?: getString(R.string.time_zone_unknown)
    }

    inner class UpdateRequestReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(C.TAG, intent.action)
            when(intent.action) {
                ACTION_TIME_ZONE_CHANGED -> {
                    timeZone = TimeZone.getTimeZone(intent.getStringExtra(TIME_ZONE_CHANGED_KEY))
                    updateStandardTimeZone()
                    clockListAdapter.notifyItemRangeChanged(0, clockItems.count())
                }
                ACTION_TIME_ZONE_SELECTOR_CHANGED -> {
                    mTimeZoneSelectorOption = mPrefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD
                }
            }
        }

    }

    companion object {
        const val TIME_ZONE_NEW_KEY = "REQUEST_KEY"
        const val TIME_ZONE_CHANGED_KEY = "TIME_ZONE_ID"
        const val USER_SELECTED_TIMEZONE = "USER_SELECTED_TIMEZONE"
        const val USER_SELECTED_DATE_AND_TIME = "USER_SELECTED_DATE_AND_TIME"
        const val ACTION_TIME_ZONE_CHANGED = "com.simples.j.worldtimealarm.APP_TIMEZONE_CHANGED"
        const val ACTION_TIME_ZONE_SELECTOR_CHANGED = "com.simples.j.worldtimealarm.APP_TIMEZONE_SELECTOR_CHANGED"

        private const val TAG_FRAGMENT_TIME_DIALOG = "TAG_FRAGMENT_TIME_DIALOG"
        private const val TAG_FRAGMENT_DATE_DIALOG = "TAG_FRAGMENT_DATE_DIALOG"
    }
}
