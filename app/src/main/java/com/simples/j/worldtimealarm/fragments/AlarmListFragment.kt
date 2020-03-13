package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.support.AlarmListAdapter
import com.simples.j.worldtimealarm.utils.*
import kotlinx.android.synthetic.main.fragment_alarm_list.*
import kotlinx.coroutines.*
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
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var alarmController: AlarmController
    private lateinit var audioManager: AudioManager
    private lateinit var fragmentLayout: CoordinatorLayout

    private var alarmItems = ArrayList<AlarmItem>()
    private var snackBar: Snackbar? = null
    private var muteStatusIsShown = false

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_alarm_list, container, false)
        fragmentLayout = view.findViewById(R.id.fragment_list)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateRequestReceiver = UpdateRequestReceiver()
        dbCursor = DatabaseCursor(fragmentContext)
        alarmController = AlarmController.getInstance()
        audioManager = fragmentContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        job = launch(coroutineContext) {
            withContext(Dispatchers.IO) {
                alarmItems = dbCursor.getAlarmList()
            }

            alarmListAdapter = AlarmListAdapter(
                    alarmItems,
                    fragmentContext).apply {
                setOnItemListener(this@AlarmListFragment)
                setHasStableIds(true)
            }
            recyclerLayoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

            alarmList.apply {
                layoutManager = recyclerLayoutManager
                adapter = alarmListAdapter
                addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }

            swipeController = ListSwipeController()
            swipeController.setOnSwipeListener(this@AlarmListFragment)

            swipeHelper = ItemTouchHelper(swipeController)
            swipeHelper.attachToRecyclerView(alarmList)
            setEmptyMessage()
            progressBar.visibility = View.GONE
        }

        // If alarm volume is muted, show snackBar
        if(savedInstanceState != null) muteStatusIsShown = savedInstanceState.getBoolean(STATE_SNACK_BAR)
        val muted = getString(R.string.volume_is_muted)
        val unMute = getString(R.string.unmute)
        if(!muteStatusIsShown) {
            if(audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                Handler().postDelayed({
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

        new_alarm.setOnClickListener {
            startActivityForResult(Intent(fragmentContext, AlarmGeneratorActivity::class.java), REQUEST_CODE_NEW)
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
                val id = this.getInt(HIGHLIGHT_KEY)
                if(id > 0) {
                    // highlight item
                    if(::alarmListAdapter.isInitialized) {
                        val index = alarmItems.indexOfFirst { it.notiId == id }
                        if(index > -1) {
                            alarmList?.smoothScrollToPosition(index)
                            alarmListAdapter.setHighlightId(id)
                            alarmListAdapter.notifyItemChanged(index)
                        }
                    }
                    else {
                        launch(coroutineContext) {
                            job.join()

                            val index = alarmItems.indexOfFirst { it.notiId == id }
                            if(index > -1) {
                                alarmList?.smoothScrollToPosition(index)
                                alarmListAdapter.setHighlightId(id)
                                alarmListAdapter.notifyItemChanged(index)
                            }
                        }
                    }
                    arguments?.remove(HIGHLIGHT_KEY) // remove id from bundle to prevent to be called again.
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if(::updateRequestReceiver.isInitialized)
            fragmentContext.unregisterReceiver(updateRequestReceiver)
        else {
            launch(coroutineContext) {
                job.cancelAndJoin()

                fragmentContext.unregisterReceiver(updateRequestReceiver)
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
                            alarmList?.scrollToPosition(alarmItems.size - 1)
                        }
                        else {
                            launch(coroutineContext) {
                                job.join()
                                alarmList?.scrollToPosition(alarmItems.size - 1)
                            }
                        }
                        setEmptyMessage()
                        showSnackBar(scheduledTime)
                    }
                }
            }
            requestCode == REQUEST_CODE_MODIFY && resultCode == Activity.RESULT_OK -> {
                val bundle = data?.getBundleExtra(AlarmReceiver.OPTIONS)
                bundle?.let { b ->
                    val item = b.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
                    val scheduledTime = b.getLong(AlarmActivity.SCHEDULED_TIME, -1)
                    if(item != null) {
                        if(::alarmListAdapter.isInitialized) {
                            val index = alarmItems.indexOfFirst { it.notiId == item.notiId }

                            if(index > -1) {
                                alarmList?.scrollToPosition(index)
                                alarmItems[index] = item
                                alarmListAdapter.notifyItemChanged(index)
                            }
                        }
                        else {
                            launch(coroutineContext) {
                                job.join()

                                val index = alarmItems.indexOfFirst { it.notiId == item.notiId }
                                if(index > -1) alarmList?.scrollToPosition(index)
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

    override fun onItemClicked(view: View, item: AlarmItem) {
        val intent = Intent(fragmentContext, AlarmGeneratorActivity::class.java)
        val bundle = Bundle().apply {
            putParcelable(AlarmReceiver.ITEM, item)
        }
        intent.putExtra(AlarmGeneratorActivity.BUNDLE_KEY, bundle)
        startActivityForResult(intent, REQUEST_CODE_MODIFY)
    }

    override fun onItemStatusChanged(b: Boolean, item: AlarmItem) {
        if(b) {
            val scheduledTime = alarmController.scheduleAlarm(fragmentContext, item, AlarmController.TYPE_ALARM)
            alarmItems.find { it.notiId == item.notiId }?.on_off = 1
            dbCursor.updateAlarmOnOffByNotiId(item.notiId, true)
            showSnackBar(scheduledTime)
        }
        else {
            alarmItems.find { it.notiId == item.notiId }?.on_off = 0
            dbCursor.updateAlarmOnOffByNotiId(item.notiId, false)
            alarmController.cancelAlarm(fragmentContext, item.notiId)
            snackBar?.dismiss()

            if(WakeUpService.isWakeUpServiceRunning && WakeUpService.currentAlarmItemId == item.notiId) {
                fragmentContext.stopService(Intent(fragmentContext, WakeUpService::class.java))
            }
        }
    }

    override fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val itemPosition = viewHolder.adapterPosition
        val previousPosition = recyclerLayoutManager.findFirstCompletelyVisibleItemPosition()
        val removedItem: AlarmItem

        alarmItems[itemPosition].let {
            removedItem = it
            if(it.on_off == 1) alarmController.cancelAlarm(fragmentContext, it.notiId)
            alarmListAdapter.removeItem(itemPosition)
            dbCursor.removeAlarm(it.notiId)
            setEmptyMessage()
        }

        Snackbar.make(fragmentLayout, resources.getString(R.string.alarm_removed), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            removedItem.let {
                val id = dbCursor.insertAlarm(it)
                it.id = id.toInt()
                if(removedItem.on_off == 1) alarmController.scheduleAlarm(fragmentContext, it, AlarmController.TYPE_ALARM)
                alarmListAdapter.addItem(itemPosition, it)
                recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
                setEmptyMessage()
            }
        }.show()
    }

    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(alarmItems,  from, to)
        alarmListAdapter.notifyItemMoved(from, to)
        dbCursor.swapAlarmOrder(alarmItems[from], alarmItems[to])
        val tmp = alarmItems[from].index
        alarmItems[from].index = alarmItems[to].index
        alarmItems[to].index = tmp
    }

    private fun setEmptyMessage() {
        if(alarmItems.size < 1) {
            alarmList.visibility = View.GONE
            list_empty.visibility = View.VISIBLE
        }
        else {
            alarmList.visibility = View.VISIBLE
            list_empty.visibility = View.GONE
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
                    if(::alarmListAdapter.isInitialized) {
                        alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                    }
                    else {
                        launch(coroutineContext) {
                            job.join()
                            alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                        }
                    }
                }
                MainActivity.ACTION_RESCHEDULE_ACTIVATED -> {
                    launch(coroutineContext) {
                        dbCursor.getActivatedAlarms().forEach {
                            alarmController.cancelAlarm(context, it.notiId)
                            alarmController.scheduleAlarm(context, it, AlarmController.TYPE_ALARM)

                            if(::alarmListAdapter.isInitialized) {
                                alarmListAdapter.readPreferences()
                                alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                            }
                            else {
                                job.join()
                                alarmListAdapter.readPreferences()
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
                if(item.repeat.any { it > 0 }) {
                    if(item.on_off == 0) alarmItems[index].on_off= 0
                }
                else {
                    // One time alarm
                    alarmItems[index].on_off = 0
                }
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

        @JvmStatic
        fun newInstance() = AlarmListFragment()
    }
}
