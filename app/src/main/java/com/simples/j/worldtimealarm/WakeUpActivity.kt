package com.simples.j.worldtimealarm

import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.ads.consent.ConsentInformation
import com.google.ads.consent.ConsentStatus
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.databinding.ActivityWakeUpBinding
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.*
import java.util.*

class WakeUpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var notificationManager: NotificationManager
    private lateinit var db: AppDatabase
    private lateinit var sharedPref: SharedPreferences
    private lateinit var binding: ActivityWakeUpBinding

    private var item: AlarmItem? = null
    private var actionBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWakeUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!intent.hasExtra(AlarmReceiver.OPTIONS)) {
            Log.d(C.TAG, "Received Alarm came without information, Finish activity.")
            finish()
        }

        val consentInfo = ConsentInformation.getInstance(this)
        if(consentInfo.consentStatus == ConsentStatus.UNKNOWN) {
            // show toast
            Toast.makeText(this, "Please launch app to set EU", Toast.LENGTH_LONG).show()
        }

        MobileAds.setRequestConfiguration(C.getAdsTestConfig())
        MobileAds.initialize(this)

        binding.adViewWakeUp.loadAd(AdRequest.Builder().build())

        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val in24Hour = sharedPref.getBoolean(applicationContext.getString(R.string.setting_24_hr_clock_key), false)

        binding.clock.format12Hour = MediaCursor.getLocalizedTimeFormat(in24Hour)
        binding.clockDate.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")

        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        item = if(option != null && !option.isEmpty) option.getParcelable(AlarmReceiver.ITEM) else null

        item.let {
            Log.d(C.TAG, "Alarm alerted : ID(${it?.notiId?.plus(1)})")

            if(it == null) {
                binding.label.visibility = View.VISIBLE
                binding.label.text = getString(R.string.error_message)
            }
            else {
                // Show selected time zone's time, but not print if time zone is default
                val timeZone = it.timeZone.replace(" ", "_")
                if(TimeZone.getDefault().id != TimeZone.getTimeZone(timeZone).id) {
                    binding.timeZoneClockLayout.visibility = View.VISIBLE
                    val name = getNameForTimeZone(it.timeZone)
                    binding.timeZoneClockTitle.text = name

                    binding.timeZoneClockTime.timeZone = timeZone
                    binding.timeZoneClockTime.format12Hour = MediaCursor.getLocalizedTimeFormat(in24Hour)
                    binding.timeZoneClockDate.timeZone = timeZone
                    binding.timeZoneClockDate.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")
                }

                // Show label
                if(!it.label.isNullOrEmpty()) {
                    binding.label.visibility = View.VISIBLE
                    binding.label.text = it.label
                    binding.label.movementMethod = ScrollingMovementMethod()
                }

                if(it.snooze > 0) {
                    binding.snooze.visibility = View.VISIBLE
                }
                else {
                    binding.snooze.visibility = View.GONE
                }
            }
        }

        binding.snooze.setOnClickListener(this)
        binding.dismiss.setOnClickListener(this)

        actionBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Can be extended later, for now this broadcast receiver is only for finish activity.
                finish()
            }
        }
        val actionIntentFilter = IntentFilter().apply {
            addAction(ACTION_ACTIVITY_FINISH)
        }

        registerReceiver(actionBroadcastReceiver, actionIntentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.cancel()
        }
        else {
            @Suppress("DEPRECATION")
            val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }

        stopService(Intent(applicationContext, WakeUpService::class.java))

        try {
            unregisterReceiver(actionBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Toast.makeText(applicationContext, getString(R.string.alarm_dismissed), Toast.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.dismiss -> {
                finish()
            }
            R.id.snooze -> {
                AlarmController.getInstance().scheduleLocalAlarm(applicationContext, item, AlarmController.TYPE_SNOOZE)

                val minutes = getString(R.string.minutes, item?.snooze?.div((60 * 1000)))
                Toast.makeText(applicationContext, getString(R.string.alarm_on, minutes), Toast.LENGTH_SHORT).show()

                val serviceActionIntent = Intent(WakeUpService.REQUEST_SERVICE_ACTION).apply { putExtra(WakeUpService.SERVICE_ACTION, AlarmReceiver.ACTION_SNOOZE) }
                sendBroadcast(serviceActionIntent)
                finish()
            }
        }
    }

    private fun getNameForTimeZone(timeZoneId: String?): String {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(timeZoneId))
        }
        else timeZoneId?.split("/").let {
            if(it != null) {
                if(it.size > 1) it[it.lastIndex]
                else it[0]
            }
            else null
        }?.replace("_", " ") ?: getString(R.string.time_zone_unknown)
    }

    companion object {
//        const val ALARM_NOTIFICATION_ID = 0
//        const val SHARED_ALARM_NOTIFICATION_ID = 132562

        const val ACTION_ACTIVITY_FINISH = "ACTION_ACTIVITY_FINISH"
    }

}
