package com.simples.j.worldtimealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.ALARM_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.DEFAULT_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.EXPIRED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.MISSED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import com.simples.j.worldtimealarm.receiver.MultiBroadcastReceiver
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope, BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var preference: SharedPreferences
    private lateinit var alarmListFragment: AlarmListFragment
    private lateinit var clockListFragment: WorldClockFragment
    private lateinit var settingFragment: SettingFragment
    private lateinit var consentForm: ConsentForm

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        createNotificationChannels()

        // EU Consent
        consentForm = MediaCursor.getConsentForm(this, ConsentListener())
        ConsentInformation.getInstance(this@MainActivity).apply {
            addTestDevice("0AD9CDC9B7C888D7B3E986949DBFC66D") // real
            addTestDevice("8E6D92F7C7055016AD3510BBB1762671") // emulator
            debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA

            requestConsentInfoUpdate(arrayOf("pub-1459869098528763"), object : ConsentInfoUpdateListener {
                override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
                    // User's consent status successfully updated
                    when(consentStatus) {
                        ConsentStatus.PERSONALIZED, ConsentStatus.NON_PERSONALIZED -> {
                            initAdmob(consentStatus)
                        }
                        ConsentStatus.UNKNOWN -> {
                            if(isRequestLocationInEeaOrUnknown) consentForm.load()
                            else initAdmob()
                        }
                        else -> {
                            Log.d(C.TAG, "Couldn't figure out consent status")
                            if(isRequestLocationInEeaOrUnknown) consentForm.load()
                            else initAdmob()
                        }
                    }
                }

                override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                    // User's consent status failed to update.
                    Log.d(C.TAG, "error: $errorDescription")
                }
            })
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

    private fun initAdmob(consentStatus: ConsentStatus = ConsentStatus.PERSONALIZED) {
        MobileAds.setRequestConfiguration(C.getAdsTestConfig())
        MobileAds.initialize(this@MainActivity)

        val builder = AdRequest.Builder()

        // put non-personalized ads flag before request
        if(consentStatus == ConsentStatus.NON_PERSONALIZED) {
            val bundle = Bundle().apply {
                putString("npa", "1")
            }
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, bundle)
        }
        adViewMain.loadAd(builder.build())
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
        intent?.getBundleExtra(MultiBroadcastReceiver.BUNDLE)?.let { bundle ->
            navigationView.selectedItemId = R.id.view_alarm
            bundle.getString(AlarmItem.WARNING, null)?.let {
                alarmListFragment.arguments = bundle
            }
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !preference.getBoolean(PREF_NOTIFICATION_CHANNEL, false)) {

            val defaultChannel = NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL, getString(R.string.default_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }

            val alarmChannel = NotificationChannel(ALARM_NOTIFICATION_CHANNEL, getString(R.string.alarm_notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(false)
                setSound(null, null)
            }

            val missedChannel = NotificationChannel(MISSED_NOTIFICATION_CHANNEL, getString(R.string.missed_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }

            val expiredChannel = NotificationChannel(EXPIRED_NOTIFICATION_CHANNEL, getString(R.string.expired_notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }

            notificationManager.createNotificationChannels(listOf(defaultChannel, alarmChannel, missedChannel, expiredChannel))
            preference.edit().putBoolean(PREF_NOTIFICATION_CHANNEL, true).apply()
        }
    }

    inner class ConsentListener(): ConsentFormListener() {
        override fun onConsentFormLoaded() {
            super.onConsentFormLoaded()
            Log.d(C.TAG, "Consent form loaded")

            try {
                consentForm.show()
            } catch (e: Exception) {
                e.printStackTrace()
                crashlytics.recordException(e.fillInStackTrace())
            }
        }

        override fun onConsentFormOpened() {
            super.onConsentFormOpened()
            Log.d(C.TAG, "Consent form opened")
        }

        override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
            super.onConsentFormClosed(consentStatus, userPrefersAdFree)
            Log.d(C.TAG, "Consent form closed")

            when(consentStatus) {
                ConsentStatus.PERSONALIZED -> {
                    initAdmob()
                }
                ConsentStatus.NON_PERSONALIZED -> {
                    initAdmob(ConsentStatus.NON_PERSONALIZED)
                }
                ConsentStatus.UNKNOWN -> { }
                else -> {
                    // unknown
                }
            }
        }

        override fun onConsentFormError(reason: String?) {
            super.onConsentFormError(reason)
            Log.d(C.TAG, "Consent form error: $reason")
        }
    }

    companion object {
        const val ACTION_UPDATE_SINGLE = "com.simples.j.world_time_alarm.ACTION_UPDATE_SINGLE"
        const val ACTION_UPDATE_ALL = "com.simples.j.world_time_alarm.ACTION_UPDATE_ALL"
        const val ACTION_RESCHEDULE_ACTIVATED = "com.simples.j.world_time_alarm.ACTION_RESCHEDULE_ACTIVATED"

        const val PREF_NOTIFICATION_CHANNEL = "PREF_NOTIFICATION_CHANNEL"
    }
}
