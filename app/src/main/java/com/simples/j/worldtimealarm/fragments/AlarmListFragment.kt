package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.*
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.*
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.databinding.FragmentAlarmListBinding
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.AlarmStatus
import com.simples.j.worldtimealarm.etc.AlarmWarningReason
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.receiver.MultiBroadcastReceiver
import com.simples.j.worldtimealarm.support.AlarmListAdapter
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

/**
 * A simple [Fragment] subclass.
 *
 */
class AlarmListFragment : Fragment(), AlarmListAdapter.OnItemClickListener, ListSwipeController.OnListControlListener, CoroutineScope {

    private lateinit var fragmentContext: Context
    private lateinit var alarmListAdapter: AlarmListAdapter
    private lateinit var updateRequestReceiver: UpdateRequestReceiver
    private lateinit var swipeHelper: ItemTouchHelper
    private lateinit var swipeController: ListSwipeController
    private lateinit var db: AppDatabase
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var alarmController: AlarmController
    private lateinit var audioManager: AudioManager
    private lateinit var fragmentLayout: CoordinatorLayout
    private lateinit var preference: SharedPreferences
    private lateinit var binding: FragmentAlarmListBinding

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private var alarmItems = ArrayList<AlarmItem>()
    private var snackBar: Snackbar? = null
    private var muteStatusIsShown = false

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmListBinding.inflate(inflater, container, false)
        fragmentLayout = binding.fragmentList

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateRequestReceiver = UpdateRequestReceiver()
        db = Room.databaseBuilder(fragmentContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
        alarmController = AlarmController.getInstance()
        audioManager = fragmentContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        preference = PreferenceManager.getDefaultSharedPreferences(fragmentContext)

        job = launch(coroutineContext) {
            setUpAlarmList()
        }

        // If alarm volume is muted, show snackBar
        if(savedInstanceState != null) muteStatusIsShown = savedInstanceState.getBoolean(STATE_SNACK_BAR)
        val muted = getString(R.string.volume_is_muted)
        val unMute = getString(R.string.unmute)
        if(!muteStatusIsShown) {
            if(audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Snackbar.make(fragmentLayout, muted, Snackbar.LENGTH_LONG)
                            .setAction(unMute) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) * 60) / 100, 0)
                    }.addCallback(object: Snackbar.Callback() {
                        override fun onShown(sb: Snackbar?) {
                            super.onShown(sb)
                            muteStatusIsShown = true
                        }
                    }).show()
                }, 500)
            }
        }

        binding.newAlarm.setOnClickListener {
            startActivityForResult(Intent(fragmentContext, AlarmGeneratorActivity::class.java), REQUEST_CODE_NEW)
        }
        binding.retry.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            showMessage(TYPE_NOTHING)
            job = launch(coroutineContext) {
                setUpAlarmList()
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.ACTION_UPDATE_ALL)
        intentFilter.addAction(MainActivity.ACTION_UPDATE_SINGLE)
        intentFilter.addAction(MainActivity.ACTION_RESCHEDULE_ACTIVATED)

        fragmentContext.registerReceiver(updateRequestReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()

        with(arguments) {
            if(this != null) {
                val id = this.getInt(HIGHLIGHT_KEY, -1)
                if(id > 0) {
                    // highlight item
                    launch(coroutineContext) {
                        job.join()

                        val index = alarmItems.indexOfFirst { it.notiId == id }
                        if(index > -1) {
                            binding.alarmList.smoothScrollToPosition(index)
                            alarmListAdapter.setHighlightId(id)
                            alarmListAdapter.notifyItemChanged(index)
                        }
                    }
                    // remove id from bundle to prevent to be called again.
                    arguments?.remove(HIGHLIGHT_KEY)
                }

                this.getString(AlarmItem.WARNING, null)?.let {
                    val warning = it.split(",")
                    val reason = this.getString(AlarmItem.REASON, null)
                            .split(",")
                            .map { r -> AlarmWarningReason.valueOf(r.toIntOrNull()) }

                    val warnings = warning.mapIndexed { index, s ->
                        Pair(s, reason[index])
                    }

                    if(::alarmListAdapter.isInitialized) {
                        alarmListAdapter.setWarningMark(warnings)
                    }
                    else {
                        launch(coroutineContext) {
                            job.join()
                            alarmListAdapter.setWarningMark(warnings)
                        }
                    }

                    arguments?.remove(AlarmItem.WARNING)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        launch(coroutineContext) {
            job.cancelAndJoin()

            if(::updateRequestReceiver.isInitialized) {
                try {
                    fragmentContext.unregisterReceiver(updateRequestReceiver)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == REQUEST_CODE_NEW && resultCode == Activity.RESULT_OK -> {
                val bundle = data?.getBundleExtra(AlarmReceiver.OPTIONS)
                bundle?.let {
                    val item = it.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
                    val scheduledTime = it.getLong(AlarmActivity.SCHEDULED_TIME, -1)
                    if(item != null) {
                        alarmItems.add(item)
                        if(::alarmListAdapter.isInitialized) {
                            alarmListAdapter.notifyItemInserted(alarmItems.size - 1)
                            binding.alarmList.scrollToPosition(alarmItems.size - 1)
                        }
                        else {
                            launch(coroutineContext) {
                                job.join()
                                binding.alarmList.scrollToPosition(alarmItems.size - 1)
                            }
                        }

                        showSnackBar(scheduledTime)
                        if(!job.isCancelled) showMessage(TYPE_RECYCLER_VIEW)
                        else {
                            binding.progressBar.visibility = View.VISIBLE
                            showMessage(TYPE_NOTHING)
                            job = launch(coroutineContext) {
                                setUpAlarmList()
                            }
                        }
                    }
                }
            }
            requestCode == REQUEST_CODE_MODIFY && resultCode == Activity.RESULT_OK -> {
                val bundle = data?.getBundleExtra(AlarmReceiver.OPTIONS)
                bundle?.let { b ->
                    val item = b.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
                    val scheduledTime = b.getLong(AlarmActivity.SCHEDULED_TIME, -1)
                    if(item != null) {
                        launch(coroutineContext) {
                            job.join()

                            val index = alarmItems.indexOfFirst { it.notiId == item.notiId }
                            if(index > -1) {
                                binding.alarmList.scrollToPosition(index)
                                alarmItems[index] = item
                                alarmListAdapter.notifyItemChanged(index)
                            }
                        }
                        showSnackBar(scheduledTime)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SNACK_BAR, muteStatusIsShown)
    }

    override fun onItemClicked(view: View, item: AlarmItem, status: AlarmStatus) {
        when(status) {
            AlarmStatus.STATUS_V22_UPDATE_ERROR -> {
                val startDate = item.startDate?.let { if(it > 0) ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) else null }
                val endDate = item.endDate?.let { if(it > 0) ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) else null }

                val date = AlarmStringFormatHelper.getDisplayLocalDate(
                        fragmentContext,
                        startDate,
                        endDate,
                        item.hasRepeatDay())

                val applyRepeat = preference.getBoolean(getString(R.string.setting_time_zone_affect_repetition_key), false)

                val oldStringBuilder = StringBuilder()
                val newStringBuilder = StringBuilder()

                val oldResult =
                        try {
                            val oldDateTimeMillis = alarmController.calculateDate(item, AlarmController.TYPE_ALARM, applyRepeat)
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(oldDateTimeMillis.timeInMillis), ZoneId.systemDefault())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.pickerTime), ZoneId.systemDefault())
                        }
                val oldRepeat = AlarmStringFormatHelper.getDisplayLocalRepeatArray(
                        fragmentContext,
                        item.repeat,
                        oldResult,
                        item.timeZone,
                        applyRepeat
                )

                val newResult = alarmController.calculateDateTime(item, AlarmController.TYPE_ALARM).withZoneSameInstant(ZoneId.systemDefault())
                val newRepeat = AlarmStringFormatHelper.getDisplayLocalRepeatArray(
                        fragmentContext,
                        item.repeat,
                        newResult,
                        item.timeZone
                )

                oldStringBuilder.appendLine(oldResult.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                date?.let { oldStringBuilder.appendLine(it) }
                oldStringBuilder.append(oldRepeat)

                newStringBuilder.appendLine(newResult.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                date?.let { newStringBuilder.appendLine(it) }
                newStringBuilder.append(newRepeat)

                val title = getString(R.string.v22_change_dialog_title)
                val msg = getString(
                        R.string.v22_change_dialog_message,
                        oldStringBuilder.toString(),
                        newStringBuilder.toString()
                )
                SimpleDialogFragment.newInstance(title, msg, SimpleDialogFragment.CANCELABLE_NO_BUTTON).show(parentFragmentManager, SimpleDialogFragment.TAG)
            }
            else -> {
                val intent = Intent(fragmentContext, AlarmGeneratorActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable(AlarmReceiver.ITEM, item)
                    putSerializable(AlarmItem.ALARM_ITEM_STATUS, status)
                }
                intent.putExtra(AlarmGeneratorActivity.BUNDLE_KEY, bundle)
                startActivityForResult(intent, REQUEST_CODE_MODIFY)
            }
        }
    }

    override fun onItemStatusChanged(b: Boolean, item: AlarmItem) {
        launch(coroutineContext) {
            if(b) {
                alarmItems.find { it.notiId == item.notiId }?.on_off = 1
                val scheduledTime = alarmController.scheduleLocalAlarm(fragmentContext, item, AlarmController.TYPE_ALARM)

                item.apply {
                    on_off = 1
                }.also {
                    db.alarmItemDao().update(it)
                }
                showSnackBar(scheduledTime)
            }
            else {
                alarmItems.find { it.notiId == item.notiId }?.on_off = 0
                item.apply {
                    on_off = 0
                }.also {
                    db.alarmItemDao().update(it)
                }
                alarmController.cancelAlarm(fragmentContext, item.notiId)
                snackBar?.dismiss()

                if(WakeUpService.isWakeUpServiceRunning && WakeUpService.currentAlarmItemId == item.notiId) {
                    fragmentContext.stopService(Intent(fragmentContext, WakeUpService::class.java))
                }
            }
        }
    }

    override fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val itemPosition = viewHolder.bindingAdapterPosition
        val previousPosition = recyclerLayoutManager.findFirstCompletelyVisibleItemPosition()
        val removedItem: AlarmItem

        alarmItems[itemPosition].let {
            removedItem = it
            if(it.on_off == 1) alarmController.cancelAlarm(fragmentContext, it.notiId)
            alarmListAdapter.removeItem(itemPosition)
            launch(coroutineContext) { db.alarmItemDao().delete(it.notiId) }
        }
        if(alarmItems.isEmpty()) showMessage(TYPE_EMPTY)

        Snackbar.make(fragmentLayout, resources.getString(R.string.alarm_removed), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            removedItem.let {
                launch(coroutineContext) {
                    // add delete item to db
                    val newId = db.alarmItemDao().insert(it)
                    it.apply {
                        id = newId.toInt()
                    }.also {
                        if(it.on_off == 1) alarmController.scheduleLocalAlarm(fragmentContext, it, AlarmController.TYPE_ALARM)

                        // add deleted item to list
                        alarmListAdapter.addItem(itemPosition, it)
                        recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
                    }
                }
            }

            showMessage(TYPE_RECYCLER_VIEW)
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        launch(coroutineContext) {
            Collections.swap(alarmItems, from, to)
            alarmListAdapter.notifyItemMoved(from, to)
            // update item index
            val tmp = alarmItems[from].index
            alarmItems[from].apply {
                index = alarmItems[to].index
            }.also {
                db.alarmItemDao().update(it)
            }
            alarmItems[to].apply {
                index = tmp
            }.also {
                db.alarmItemDao().update(it)
            }
        }
    }

    private suspend fun setUpAlarmList() {
        withContext(Dispatchers.IO) {
            alarmItems = ArrayList(db.alarmItemDao().getAll())
        }

        alarmListAdapter = AlarmListAdapter(
                alarmItems,
                fragmentContext).apply {
            setOnItemListener(this@AlarmListFragment)
            setHasStableIds(true)
        }
        recyclerLayoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

        binding.alarmList.apply {
            layoutManager = recyclerLayoutManager
            adapter = alarmListAdapter
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

            if(itemDecorationCount <= 0) addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
        }

        if(!::swipeController.isInitialized) {
            swipeController = ListSwipeController()
            swipeController.setOnSwipeListener(this@AlarmListFragment)
        }

        if(!::swipeHelper.isInitialized) {
            swipeHelper = ItemTouchHelper(swipeController)
            swipeHelper.attachToRecyclerView(binding.alarmList)
        }

        binding.progressBar.visibility = View.GONE
        if(alarmItems.isEmpty()) showMessage(TYPE_EMPTY)
        else showMessage(TYPE_RECYCLER_VIEW)
    }

    // TODO: Item is not visible when visibility is set to GONE
    //  after item restored after remove for only first item is on the list
    private fun showMessage(type: Int) {
        when(type) {
            TYPE_EMPTY -> {
                binding.alarmList.visibility = View.INVISIBLE
                binding.listEmpty.visibility = View.VISIBLE
                binding.retry.visibility = View.INVISIBLE
            }
            TYPE_RETRY -> {
                binding.alarmList.visibility = View.INVISIBLE
                binding.listEmpty.visibility = View.INVISIBLE
                binding.retry.visibility = View.VISIBLE
            }
            TYPE_RECYCLER_VIEW -> {
                binding.alarmList.visibility = View.VISIBLE
                binding.listEmpty.visibility = View.INVISIBLE
                binding.retry.visibility = View.INVISIBLE
            }
            else -> {
                binding.alarmList.visibility = View.INVISIBLE
                binding.listEmpty.visibility = View.INVISIBLE
                binding.retry.visibility = View.INVISIBLE
            }
        }
    }

    private fun showSnackBar(scheduledTime: Long) {
        if(scheduledTime != -1L) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = scheduledTime
            }
            snackBar = Snackbar.make(fragmentLayout, getString(R.string.alarm_on, MediaCursor.getRemainTime(fragmentContext, calendar)), Snackbar.LENGTH_LONG)
            snackBar?.show()
        }
    }

    private inner class UpdateRequestReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                MainActivity.ACTION_UPDATE_SINGLE -> {
                    val bundle = intent.getBundleExtra(AlarmReceiver.OPTIONS)
                    val item = bundle?.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
                    if(item == null) {
                        Log.d(C.TAG, "AlarmItem is null")
                        return
                    }
                    if(::alarmListAdapter.isInitialized) {
                        updateList(item)
                    }
                    else {
                        launch(coroutineContext) {
                            job.join()
                            updateList(item)
                        }
                    }
                }
                MainActivity.ACTION_UPDATE_ALL -> {
                    val bundle = intent.getBundleExtra(MultiBroadcastReceiver.BUNDLE)
                    if(::alarmListAdapter.isInitialized) {
                        bundle?.getString(AlarmItem.WARNING, null)?.let {
                            val warning = it.split(",")
                            val reason = bundle.getString(AlarmItem.REASON, null)
                                    .split(",")
                                    .map { r -> AlarmWarningReason.valueOf(r.toIntOrNull()) }

                            val warnings = warning.mapIndexed { index, s ->
                                Pair(s, reason[index])
                            }
                            alarmListAdapter.setWarningMark(warnings)
                        }
                        alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                    }
                    else {
                        launch(coroutineContext) {
                            job.join()
                            bundle?.getString(AlarmItem.WARNING, null)?.let {
                                val warning = it.split(",")
                                val reason = bundle.getString(AlarmItem.REASON, null)
                                        .split(",")
                                        .map { r -> AlarmWarningReason.valueOf(r.toIntOrNull()) }

                                val warnings = warning.mapIndexed { index, s ->
                                    Pair(s, reason[index])
                                }
                                alarmListAdapter.setWarningMark(warnings)
                            }
                            alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                        }
                    }
                }
                MainActivity.ACTION_RESCHEDULE_ACTIVATED -> {
                    launch(coroutineContext) {
                        db.alarmItemDao().getActivated().forEach {
                            alarmController.cancelAlarm(context, it.notiId)
                            alarmController.scheduleLocalAlarm(context, it, AlarmController.TYPE_ALARM)

                            if(::alarmListAdapter.isInitialized) {
                                alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                            }
                            else {
                                job.join()
                                alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                            }
                        }
                    }
                }
            }
        }

        private fun updateList(item: AlarmItem) {
            val index = alarmItems.indexOfFirst { it.notiId == item.notiId }
            if(index > -1) {
                alarmItems[index] = item
                alarmListAdapter.notifyItemChanged(index)
            }
        }
    }

    companion object {
        const val TAG = "AlarmListFragment"
        const val REQUEST_CODE_NEW = 10
        const val REQUEST_CODE_MODIFY = 20
        const val STATE_SNACK_BAR = "STATE_SNACK_BAR"
        const val HIGHLIGHT_KEY = "HIGHLIGHT_KEY"

        const val TYPE_EMPTY = 0
        const val TYPE_RETRY = 1
        const val TYPE_RECYCLER_VIEW = 2
        const val TYPE_NOTHING = 3

        @JvmStatic
        fun newInstance() = AlarmListFragment()
    }
}
