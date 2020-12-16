package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_NEW_CODE
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.models.WorldClockViewModel
import com.simples.j.worldtimealarm.support.ClockListAdapter
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.ListSwipeController
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_world_clock.*
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*
import kotlin.coroutines.CoroutineContext

class WorldClockFragment : Fragment(), View.OnClickListener, ListSwipeController.OnListControlListener, CoroutineScope, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private lateinit var fragmentContext: Context
    private lateinit var viewModel: WorldClockViewModel
    private lateinit var clockListAdapter: ClockListAdapter
    private lateinit var swipeHelper: ItemTouchHelper
    private lateinit var swipeController: ListSwipeController
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var fragmentLayout: CoordinatorLayout
    private lateinit var timeZoneChangedReceiver: UpdateRequestReceiver
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var timeDialog: TimePickerDialogFragment
    private lateinit var dateDialog: DatePickerDialogFragment

    private var clockItems = ArrayList<ClockItem>()
    private var removedItem: ClockItem? = null

    private lateinit var mPrefManager: SharedPreferences
    private var mTimeZoneSelectorOption: String = ""

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)
        fragmentLayout = view.findViewById(R.id.fragment_list)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dbCursor = DatabaseCursor(fragmentContext)
        activity?.run {
            viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]
        }

        mPrefManager = PreferenceManager.getDefaultSharedPreferences(fragmentContext)
        val rememberLast = mPrefManager.getBoolean(resources.getString(R.string.setting_converter_remember_last_key), false)
        mTimeZoneSelectorOption = mPrefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD

        if(rememberLast) {
            val set = mPrefManager.getStringSet(LAST_SETTING_KEY, null)
            if(set != null) {
                var instant: Instant = Instant.now()
                var zoneId: ZoneId = ZoneId.systemDefault()
                set.forEach {
                    try {
                        instant = Instant.ofEpochMilli(it.toLong())
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                        zoneId = ZoneId.of(it)
                    }
                }
                viewModel.mainZonedDateTime.value = ZonedDateTime.ofInstant(instant, zoneId)
            }
        }

        time_zone_layout.setOnClickListener(this)
        new_timezone.setOnClickListener(this)

        timeDialog =
                parentFragmentManager.findFragmentByTag(TAG_FRAGMENT_TIME_DIALOG) as? TimePickerDialogFragment ?:
                        TimePickerDialogFragment.newInstance()
        timeDialog.setTimeSetListener(this)

        dateDialog =
                parentFragmentManager.findFragmentByTag(TAG_FRAGMENT_DATE_DIALOG) as? DatePickerDialogFragment ?:
                DatePickerDialogFragment.newInstance().apply {
                    minDate = 0
                }
        dateDialog.setDateSetListener(this)

        world_time.setOnClickListener {
            if(!timeDialog.isAdded) timeDialog.show(parentFragmentManager, TAG_FRAGMENT_TIME_DIALOG)
        }
        world_date.setOnClickListener {
            if(!dateDialog.isAdded) dateDialog.show(parentFragmentManager, TAG_FRAGMENT_DATE_DIALOG)
        }

        job = launch(coroutineContext) {
            withContext(Dispatchers.IO) {
                clockItems = dbCursor.getClockList()
            }

            clockListAdapter = ClockListAdapter(fragmentContext, clockItems, viewModel)
            recyclerLayoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

            // TODO: Fix NullPointerException at setLayoutManager()
            clockList.apply {
                layoutManager = recyclerLayoutManager
                adapter = clockListAdapter
                addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }

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
            fragmentContext.registerReceiver(timeZoneChangedReceiver, intentFilter)
        }

        viewModel.mainZonedDateTime.observe(viewLifecycleOwner, {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, it.hour)
                set(Calendar.MINUTE, it.minute)
            }

            world_time.text = DateFormat.format(MediaCursor.getLocalizedTimeFormat(), calendar)
            world_date.text = it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

            time_zone.text = getNameForTimeZone(it.zone.id)

            updateList()

            val set = setOf(it.toInstant().toEpochMilli().toString(), it.zone.id)
            mPrefManager.edit()
                    .putStringSet(LAST_SETTING_KEY, set)
                    .apply()
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        if(::timeZoneChangedReceiver.isInitialized)
            fragmentContext.unregisterReceiver(timeZoneChangedReceiver)
        else {
            launch(coroutineContext) {
                job.cancelAndJoin()

                fragmentContext.unregisterReceiver(timeZoneChangedReceiver)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val scrollToLast: Boolean

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    val id = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)?.replace(" ", "_")

                    viewModel.mainZonedDateTime.value =
                            viewModel.mainZonedDateTime.value?.withZoneSameLocal(ZoneId.of(id))
                }
            }
            requestCode == TIME_ZONE_NEW_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    dbCursor.insertClock(ClockItem(null, data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID), -1))
                    clockItems.clear()
                    clockItems.addAll(dbCursor.getClockList())
                    scrollToLast = true

                    setEmptyMessage()
                    updateList(scrollToLast)
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.time_zone_layout -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(fragmentContext, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_CHANGE)
                        putExtra(TimeZonePickerActivity.TIME_ZONE_ID, viewModel.mainZonedDateTime.value?.zone?.id)
                        putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_WORLD_CLOCK)
                    }
                    startActivityForResult(i, TIME_ZONE_REQUEST_CODE)
                }
                else startActivityForResult(Intent(activity, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
            R.id.new_timezone -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(fragmentContext, TimeZonePickerActivity::class.java).apply {
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

        removedItem?.let { clockItem ->
            dbCursor.removeClock(clockItem)
        }
        setEmptyMessage()

        Snackbar.make(fragmentLayout, resources.getString(R.string.clock_removed, getNameForTimeZone(removedItem?.timezone)), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            removedItem?.let { clockItem ->
                val id = dbCursor.insertClock(clockItem)
                clockItem.id = id.toInt()
                clockListAdapter.addItem(itemPosition, clockItem)
                recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
            }
            setEmptyMessage()
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(clockItems, from, to)
        clockListAdapter.notifyItemMoved(from, to)
        dbCursor.swapClockOrder(clockItems[from], clockItems[to])
        val tmp = clockItems[from].index
        clockItems[from].index = clockItems[to].index
        clockItems[to].index = tmp
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        viewModel.mainZonedDateTime.value =
                viewModel.mainZonedDateTime.value
                        ?.withHour(hourOfDay)
                        ?.withMinute(minute)
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        viewModel.mainZonedDateTime.value =
                viewModel.mainZonedDateTime.value
                        ?.withYear(year)
                        ?.withMonth(month+1)
                        ?.withDayOfMonth(dayOfMonth)
    }

    private fun updateList(scrollToLast: Boolean = false) {
        if(::clockListAdapter.isInitialized) {
            clockListAdapter.notifyItemRangeChanged(0, clockItems.count())

            if(clockItems.isNotEmpty() && scrollToLast)
                clockList?.smoothScrollToPosition(clockItems.count() - 1)
        }
        else {
            launch(coroutineContext) {
                job.join()
                clockListAdapter.notifyItemRangeChanged(0, clockItems.count())

                if(clockItems.isNotEmpty() && scrollToLast)
                    clockList?.smoothScrollToPosition(clockItems.count() - 1)
            }
        }
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
            when(intent.action) {
                ACTION_TIME_ZONE_CHANGED -> {
                    val zoneId = ZoneId.of(intent.getStringExtra(TIME_ZONE_CHANGED_KEY)) ?: ZoneId.systemDefault()
                    viewModel.mainZonedDateTime.value = viewModel.mainZonedDateTime.value?.withZoneSameLocal(zoneId)
                }
                ACTION_TIME_ZONE_SELECTOR_CHANGED -> {
                    mTimeZoneSelectorOption = mPrefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD
                }
                ACTION_LAST_SETTING_CHANGED -> {
                    viewModel.mainZonedDateTime.value?.let {
                        val set = setOf(it.toInstant().toEpochMilli().toString(), it.zone.id)
                        mPrefManager.edit()
                                .putStringSet(LAST_SETTING_KEY, set)
                                .apply()
                    }
                }
            }
        }

    }

    companion object {
        const val TAG = "WorldClockFragment"
        const val TIME_ZONE_NEW_KEY = "REQUEST_KEY"
        const val TIME_ZONE_CHANGED_KEY = "TIME_ZONE_ID"
        const val LAST_SETTING_KEY = "LAST_SETTING_KEY"
        const val ACTION_TIME_ZONE_CHANGED = "com.simples.j.worldtimealarm.APP_TIMEZONE_CHANGED"
        const val ACTION_TIME_ZONE_SELECTOR_CHANGED = "com.simples.j.worldtimealarm.APP_TIMEZONE_SELECTOR_CHANGED"
        const val ACTION_LAST_SETTING_CHANGED = "com.simples.j.worldtimealarm.ACTION_LAST_SETTING_CHANGED"

        private const val TAG_FRAGMENT_TIME_DIALOG = "TAG_FRAGMENT_TIME_DIALOG"
        private const val TAG_FRAGMENT_DATE_DIALOG = "TAG_FRAGMENT_DATE_DIALOG"

        @JvmStatic
        fun newInstance() = WorldClockFragment()
    }
}