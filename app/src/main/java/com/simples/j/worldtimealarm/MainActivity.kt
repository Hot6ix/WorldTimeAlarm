package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.support.AlarmListAdapter
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.ListSwipeController
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity(), AlarmListAdapter.OnItemClickListener, ListSwipeController.OnSwipeListener {

    private lateinit var alarmListAdapter: AlarmListAdapter
    private lateinit var updateRequestReceiver: UpdateRequestReceiver
    private lateinit var swipeHelper: ItemTouchHelper
    private lateinit var swipeController: ListSwipeController
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var alarmController: AlarmController
    private lateinit var audioManager: AudioManager
    private var alarmItems = ArrayList<AlarmItem>()
    private var snackBar: Snackbar? = null
    private var muteStatusIsShown = false
    private var removedItem: AlarmItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateRequestReceiver = UpdateRequestReceiver()
        swipeController = ListSwipeController()
        swipeHelper = ItemTouchHelper(swipeController)
        dbCursor = DatabaseCursor(applicationContext)
        alarmController = AlarmController.getInstance(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
        adViewMain.loadAd(AdRequest.Builder().build())

        new_alarm.setOnClickListener {
            startActivityForResult(Intent(this, AlarmActivity::class.java), REQUEST_CODE_NEW)
        }

        alarmItems = dbCursor.getAlarmList()
        alarmListAdapter = AlarmListAdapter(alarmItems, applicationContext)
        alarmListAdapter.setOnItemListener(this)
        recyclerLayoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        alarmList.layoutManager = recyclerLayoutManager
        alarmList.adapter = alarmListAdapter
        alarmList.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
        swipeHelper.attachToRecyclerView(alarmList)
        swipeController.setOnSwipeListener(this)
        setEmptyMessage()

        // If alarm volume is muted, show snackBar
        if(savedInstanceState != null) muteStatusIsShown = savedInstanceState.getBoolean(STATE_SNACKBAR)
        if(!muteStatusIsShown) {
            if(audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                Handler().postDelayed({
                    Snackbar.make(main_layout, getString(R.string.volume_is_muted), Snackbar.LENGTH_LONG).setAction(getString(R.string.unmute)) {
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

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_UPDATE_ALL)
        intentFilter.addAction(ACTION_UPDATE_SINGLE)
        registerReceiver(updateRequestReceiver, intentFilter)
//        val intent =Intent(this, WakeUpActivity::class.java)
//        val bundle = Bundle()
////        bundle.putParcelable(AlarmReceiver.ITEM, AlarmItem(99, "America/Argentina/ComodRivadavia", Calendar.getInstance().time.time.toString(), intArrayOf(0,0,0,0,0,0,0), null, null, 3000, null, 1, 78788))
//        bundle.putParcelable(AlarmReceiver.ITEM, AlarmItem(99, "Asia/Seoul", Calendar.getInstance().time.time.toString(), intArrayOf(0,0,0,0,0,0,0), null, null, 3000, null, 1, 78788))
//        intent.putExtra(AlarmReceiver.OPTIONS, bundle)
//        startActivity(intent)

    }

    override fun onRestart() {
        super.onRestart()

        if(alarmItems.size != dbCursor.getAlarmListSize().toInt()) {
            alarmItems.clear()
            alarmItems.addAll(dbCursor.getAlarmList())
            Log.d("taggg", alarmItems.size.toString())
            alarmListAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(updateRequestReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == REQUEST_CODE_NEW && resultCode == Activity.RESULT_OK -> {
                val item = data?.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)
                if(item != null) {
                    item.id = dbCursor.getAlarmId(item.notiId)
                    alarmItems.add(item)
                    alarmListAdapter.notifyItemInserted(alarmItems.size - 1)
                    alarmList.scrollToPosition(alarmItems.size - 1)
                    setEmptyMessage()
                    showSnackBar(item)
                }
            }
            requestCode == REQUEST_CODE_MODIFY && resultCode == Activity.RESULT_OK -> {
                val item = data?.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)
                if(item != null ) {
                    var index = 0
                    alarmItems.forEachIndexed { i, it ->
                        if(it.notiId == item.notiId) index = i
                    }
                    alarmItems[index] = item
                    alarmListAdapter.notifyItemChanged(index)
                    showSnackBar(item)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(STATE_SNACKBAR, muteStatusIsShown)
    }

    override fun onItemClicked(view: View, item: AlarmItem) {
        val intent = Intent(this, AlarmActivity::class.java)
        intent.putExtra(AlarmReceiver.ITEM, item)
        startActivityForResult(intent, REQUEST_CODE_MODIFY)
    }

    override fun onItemStatusChanged(b: Boolean, item: AlarmItem) {
        if(b) {
            alarmItems.find { it.notiId == item.notiId }?.on_off = 1
            dbCursor.updateAlarmOnOffByNotiId(item.notiId, true)
            alarmController.scheduleAlarm(applicationContext, item, AlarmController.TYPE_ALARM)
            showSnackBar(item)
            alarmListAdapter.notifyItemChanged(alarmItems.indexOf(item))
        }
        else {
            alarmItems.find { it.notiId == item.notiId }?.on_off = 0
            dbCursor.updateAlarmOnOffByNotiId(item.notiId, false)
            alarmController.cancelAlarm(applicationContext, item.notiId)
            snackBar?.dismiss()
        }
    }

    override fun onSwipe(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val itemPosition = viewHolder.adapterPosition
        val previousPosition = recyclerLayoutManager.findFirstCompletelyVisibleItemPosition()

        removedItem = alarmItems[itemPosition]
        if(removedItem!!.on_off == 1) alarmController.cancelAlarm(applicationContext, removedItem!!.notiId)
        alarmListAdapter.removeItem(itemPosition)
        dbCursor.removeAlarm(removedItem!!.notiId)
        setEmptyMessage()

        Snackbar.make(main_layout, resources.getString(R.string.alarm_removed), Snackbar.LENGTH_LONG).setAction(resources.getString(R.string.undo)) {
            dbCursor.insertAlarm(removedItem!!)
            if(removedItem?.on_off == 1) alarmController.scheduleAlarm(applicationContext, removedItem!!, AlarmController.TYPE_ALARM)
            alarmListAdapter.addItem(itemPosition, removedItem!!)
            recyclerLayoutManager.scrollToPositionWithOffset(previousPosition, 0)
            setEmptyMessage()
        }.show()
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

    private fun showSnackBar(item: AlarmItem) {
        val calendar = Calendar.getInstance()
        calendar.time = Date(item.timeSet.toLong())

        if(item.repeat.any { it == 1 }) {
            var days = ""
            val dayArray = resources.getStringArray(R.array.day_of_week_full)
            val repeatArray = item.repeat.mapIndexed { index, i ->
                if(i == 1) dayArray[index] else null
            }.filter { it != null }
            days = if(repeatArray.size == 7)
                getString(R.string.everyday)
            else if(repeatArray.contains(dayArray[6]) && repeatArray.contains(dayArray[0]) && repeatArray.size  == 2)
                getString(R.string.weekend)
            else if(repeatArray.contains(dayArray[1]) && repeatArray.contains(dayArray[2]) && repeatArray.contains(dayArray[3]) && repeatArray.contains(dayArray[4]) && repeatArray.contains(dayArray[5]) && repeatArray.size == 5)
                getString(R.string.weekday)
            else repeatArray.joinToString()

            snackBar = if(repeatArray.size == 7)
                Snackbar.make(main_layout, getString(R.string.alarm_on_repeat_every, DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(calendar.time)), Snackbar.LENGTH_LONG)
            else
                Snackbar.make(main_layout, getString(R.string.alarm_on_repeat, days, DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(calendar.time)), Snackbar.LENGTH_LONG)
        }
        else {
            while (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (calendar.timeInMillis - System.currentTimeMillis() > C.ONE_DAY) {
                calendar.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
            }
            snackBar = Snackbar.make(main_layout, getString(R.string.alarm_on, MediaCursor.getRemainTime(applicationContext, calendar)), Snackbar.LENGTH_LONG)
        }

        snackBar?.show()
    }

    inner class UpdateRequestReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(C.TAG, intent.action)
            when(intent.action) {
                ACTION_UPDATE_SINGLE -> {
                    val bundle = intent.getBundleExtra(AlarmReceiver.OPTIONS)
                    val item = bundle.getParcelable<AlarmItem>(AlarmReceiver.ITEM)
                    var index = 0
                    alarmItems.forEachIndexed { i, it ->
                        if(it.notiId == item.notiId) index = i
                    }

                    if(item.repeat.any { it == 1 }) {
                    }
                    else {
                        // One time alarm
                        alarmItems[index].on_off = 0
                    }
                    alarmListAdapter.notifyItemChanged(index)
                }
                ACTION_UPDATE_ALL -> {
                    alarmListAdapter.notifyItemRangeChanged(0, alarmItems.count())
                }
            }
        }

    }

    companion object {
        const val REQUEST_CODE_NEW = 10
        const val REQUEST_CODE_MODIFY = 20
        const val STATE_SNACKBAR = "STATE_SNACKBAR"
        const val ACTION_UPDATE_SINGLE = "com.simples.j.worldtimealarm.ACTION_UPDATE_SINGLE"
        const val ACTION_UPDATE_ALL = "com.simples.j.worldtimealarm.ACTION_UPDATE_ALL"
    }
}
