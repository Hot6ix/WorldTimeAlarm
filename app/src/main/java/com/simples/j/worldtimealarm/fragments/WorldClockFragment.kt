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
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_NEW_CODE
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.databinding.FragmentWorldClockBinding
import com.simples.j.worldtimealarm.etc.ClockItem
import com.simples.j.worldtimealarm.models.WorldClockViewModel
import com.simples.j.worldtimealarm.support.ClockListAdapter
import com.simples.j.worldtimealarm.utils.*
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList
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
    private lateinit var timeDialog: TimePickerDialogFragment
    private lateinit var dateDialog: DatePickerDialogFragment
    private lateinit var binding: FragmentWorldClockBinding
    private lateinit var db: AppDatabase

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private var clockItems = ArrayList<ClockItem>()
    private var removedItem: ClockItem? = null

    private lateinit var mPrefManager: SharedPreferences
    private var mTimeZoneSelectorOption: String = ""

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineExceptionHandler

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()

        crashlytics.recordException(throwable)
        binding.progressBar.visibility = View.GONE
        showMessage(TYPE_RETRY)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentWorldClockBinding.inflate(inflater, container, false)
        fragmentLayout = binding.fragmentList
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        db = Room.databaseBuilder(fragmentContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
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

        binding.timeZoneLayout.setOnClickListener(this)
        binding.newTimezone.setOnClickListener(this)

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

        binding.worldTime.setOnClickListener {
            if(!timeDialog.isAdded) timeDialog.show(parentFragmentManager, TAG_FRAGMENT_TIME_DIALOG)
        }
        binding.worldDate.setOnClickListener {
            if(!dateDialog.isAdded) dateDialog.show(parentFragmentManager, TAG_FRAGMENT_DATE_DIALOG)
        }
        binding.retry.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            showMessage(TYPE_NOTHING)
            job = launch(coroutineContext) {
                setUpClockList()
            }
        }

        job = launch(coroutineContext) {
            setUpClockList()
        }

        viewModel.mainZonedDateTime.observe(viewLifecycleOwner, {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, it.hour)
                set(Calendar.MINUTE, it.minute)
            }

            binding.worldTime.text = DateFormat.format(MediaCursor.getLocalizedTimeFormat(), calendar)
            binding.worldDate.text = it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

            binding.timeZone.text = getNameForTimeZone(it.zone.id)

            updateList()

            val set = setOf(it.toInstant().toEpochMilli().toString(), it.zone.id)
            mPrefManager.edit()
                    .putStringSet(LAST_SETTING_KEY, set)
                    .apply()
        })

        timeZoneChangedReceiver = UpdateRequestReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_TIME_ZONE_CHANGED)
            addAction(ACTION_TIME_ZONE_SELECTOR_CHANGED)
        }
        fragmentContext.registerReceiver(timeZoneChangedReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        launch(coroutineContext) {
            job.cancelAndJoin()

            if(::timeZoneChangedReceiver.isInitialized) {
                try {
                    fragmentContext.unregisterReceiver(timeZoneChangedReceiver)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var scrollToLast: Boolean

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
                    launch(coroutineContext) {
                        job.join()

                        data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)?.let {
                            val clockItem = ClockItem(null, it, -1)
                            val id = db.clockItemDao().insert(clockItem)
                            clockItem.apply {
                                index = id.toInt()
                            }.also {
                                db.clockItemDao().update(it)
                            }
                            clockItems.add(clockItem)

                            scrollToLast = true

                            updateList(scrollToLast)
                            if(!job.isCancelled) showMessage(TYPE_RECYCLER_VIEW)
                            else {
                                binding.progressBar.visibility = View.VISIBLE
                                showMessage(TYPE_NOTHING)
                                job = launch(coroutineContext) {
                                    setUpClockList()
                                }
                            }
                        }
                    }

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
        val itemPosition = viewHolder.bindingAdapterPosition
        val previousPosition = recyclerLayoutManager.findFirstCompletelyVisibleItemPosition()

        removedItem = clockItems[itemPosition]
        clockListAdapter.removeItem(itemPosition)

        removedItem?.let { clockItem ->
            launch(coroutineContext) { db.clockItemDao().delete(clockItem) }
        }
        if(clockItems.isEmpty()) showMessage(TYPE_EMPTY)

        Snackbar.make(fragmentLayout, resources.getString(R.string.clock_removed, getNameForTimeZone(removedItem?.timezone)), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            removedItem?.let { clockItem ->
                launch(coroutineContext) {
                    val newId = db.clockItemDao().insert(clockItem)
                    clockItem.apply {
                        id = newId.toInt()
                    }.also {
                        clockListAdapter.addItem(itemPosition, clockItem)
                        recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
                    }
                }
            }

            showMessage(TYPE_RECYCLER_VIEW)
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        launch(coroutineContext) {
            Collections.swap(clockItems, from, to)
            clockListAdapter.notifyItemMoved(from, to)
            // update item index
            val tmp = clockItems[from].index
            clockItems[from].apply {
                index = clockItems[to].index
            }.also {
                db.clockItemDao().update(it)
            }
            clockItems[to].apply {
                index = tmp
            }.also {
                db.clockItemDao().update(it)
            }
        }
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

    private suspend fun setUpClockList() {
        withContext(Dispatchers.IO) {
            clockItems = ArrayList(db.clockItemDao().getAll())
        }

        clockListAdapter = ClockListAdapter(fragmentContext, clockItems, viewModel)
        recyclerLayoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

        binding.clockList.apply {
            layoutManager = recyclerLayoutManager
            adapter = clockListAdapter
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

            if(itemDecorationCount <= 0) addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
        }

        if(!::swipeController.isInitialized) {
            swipeController = ListSwipeController()
            swipeController.setOnSwipeListener(this@WorldClockFragment)
        }

        if(!::swipeHelper.isInitialized) {
            swipeHelper = ItemTouchHelper(swipeController)
            swipeHelper.attachToRecyclerView(binding.clockList)
        }

        binding.progressBar.visibility = View.GONE
        if(clockItems.isEmpty()) showMessage(AlarmListFragment.TYPE_EMPTY)
        else showMessage(AlarmListFragment.TYPE_RECYCLER_VIEW)
    }

    private fun updateList(scrollToLast: Boolean = false) {
        launch(coroutineContext) {
            job.join()

            if(::clockListAdapter.isInitialized) {
                clockListAdapter.notifyItemRangeChanged(0, clockItems.count())

                if (clockItems.isNotEmpty() && scrollToLast)
                    binding.clockList.smoothScrollToPosition(clockItems.count() - 1)
            }
        }
    }

    private fun showMessage(type: Int) {
        when(type) {
            TYPE_EMPTY -> {
                binding.clockList.visibility = View.GONE
                binding.listEmpty.visibility = View.VISIBLE
                binding.retry.visibility = View.GONE
            }
            TYPE_RETRY -> {
                binding.clockList.visibility = View.GONE
                binding.listEmpty.visibility = View.GONE
                binding.retry.visibility = View.VISIBLE
            }
            TYPE_RECYCLER_VIEW -> {
                binding.clockList.visibility = View.VISIBLE
                binding.listEmpty.visibility = View.GONE
                binding.retry.visibility = View.GONE
            }
            else -> {
                binding.clockList.visibility = View.GONE
                binding.listEmpty.visibility = View.GONE
                binding.retry.visibility = View.GONE
            }
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

        const val TYPE_EMPTY = 0
        const val TYPE_RETRY = 1
        const val TYPE_RECYCLER_VIEW = 2
        const val TYPE_NOTHING = 3

        const val TAG_FRAGMENT_TIME_DIALOG = "TAG_FRAGMENT_TIME_DIALOG"
        const val TAG_FRAGMENT_DATE_DIALOG = "TAG_FRAGMENT_DATE_DIALOG"

        @JvmStatic
        fun newInstance() = WorldClockFragment()
    }
}