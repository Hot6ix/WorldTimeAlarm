package com.simples.j.worldtimealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.ALARM_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.EXPIRED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.MISSED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope, BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var preference: SharedPreferences
    private lateinit var alarmListFragment: AlarmListFragment
    private lateinit var clockListFragment: WorldClockFragment
    private lateinit var settingFragment: SettingFragment

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        createNotificationChannels()
        AndroidThreeTen.init(application)

        launch(coroutineContext) {
            withContext(Dispatchers.IO) {
                MobileAds.setRequestConfiguration(C.getAdsTestConfig())
                MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
            }
            val builder = AdRequest.Builder()
            adViewMain.loadAd(builder.build())
        }

        alarmListFragment = AlarmListFragment.newInstance()
        clockListFragment = WorldClockFragment.newInstance()
        settingFragment = SettingFragment.newInstance()

        val transaction = supportFragmentManager.beginTransaction()
        if(supportFragmentManager.findFragmentByTag(AlarmListFragment.TAG) == null) {
            transaction.add(R.id.fragment_container, alarmListFragment, AlarmListFragment.TAG)
        }
        else {
            alarmListFragment = supportFragmentManager.findFragmentByTag(AlarmListFragment.TAG) as AlarmListFragment
        }
        if(supportFragmentManager.findFragmentByTag(WorldClockFragment.TAG) == null) {
            transaction.add(R.id.fragment_container, clockListFragment, WorldClockFragment.TAG)
        }
        else {
            clockListFragment = supportFragmentManager.findFragmentByTag(WorldClockFragment.TAG) as WorldClockFragment
        }
        if(supportFragmentManager.findFragmentByTag(SettingFragment.TAG) == null) {
            transaction.add(R.id.fragment_container, settingFragment, SettingFragment.TAG)
        }
        else {
            settingFragment = supportFragmentManager.findFragmentByTag(SettingFragment.TAG) as SettingFragment
        }
        transaction.commitNow()

        sendHighlightRequest(intent)

        navigationView.setOnNavigationItemSelectedListener(this)
        if(!transaction.isEmpty) {
            navigationView.selectedItemId = R.id.view_alarm
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sendHighlightRequest(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        adViewMain.destroy()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        handleNavigationClick(item.itemId)
        return true
    }

    private fun handleNavigationClick(id: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        when(id) {
            R.id.view_alarm -> {
                transaction.hide(clockListFragment)
                transaction.hide(settingFragment)
                transaction.show(alarmListFragment)
            }
            R.id.view_clock -> {
                transaction.hide(alarmListFragment)
                transaction.hide(settingFragment)
                transaction.show(clockListFragment)
            }
            R.id.view_setting -> {
                transaction.hide(alarmListFragment)
                transaction.hide(clockListFragment)
                transaction.show(settingFragment)
            }
        }

        transaction.commitNow()
    }

    private fun sendHighlightRequest(intent: Intent?) {
        intent?.getIntExtra(AlarmListFragment.HIGHLIGHT_KEY, 0)?.let {
            if(it > 0) {
                navigationView.selectedItemId = R.id.view_alarm
                val bundle = Bundle().apply {
                    putInt(AlarmListFragment.HIGHLIGHT_KEY, it)
                }
                alarmListFragment.arguments = bundle
                intent.removeExtra(AlarmListFragment.HIGHLIGHT_KEY)
            }
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !preference.getBoolean(PREF_NOTIFICATION_CHANNEL, false)) {
            val alarmChannel = NotificationChannel(ALARM_NOTIFICATION_CHANNEL, getString(R.string.alarm_notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(false)
                setSound(null,  null)
            }

            val missedChannel = NotificationChannel(MISSED_NOTIFICATION_CHANNEL, getString(R.string.missed_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }

            val expiredChannel = NotificationChannel(EXPIRED_NOTIFICATION_CHANNEL, getString(R.string.expired_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)

            }

            notificationManager.createNotificationChannels(listOf(alarmChannel, missedChannel, expiredChannel))
            preference.edit().putBoolean(PREF_NOTIFICATION_CHANNEL, true).apply()
        }
    }

    companion object {
        const val ACTION_UPDATE_SINGLE = "com.simples.j.world_time_alarm.ACTION_UPDATE_SINGLE"
        const val ACTION_UPDATE_ALL = "com.simples.j.world_time_alarm.ACTION_UPDATE_ALL"
        const val ACTION_RESCHEDULE_ACTIVATED = "com.simples.j.world_time_alarm.ACTION_RESCHEDULE_ACTIVATED"

        const val PREF_NOTIFICATION_CHANNEL = "PREF_NOTIFICATION_CHANNEL"
    }
}
