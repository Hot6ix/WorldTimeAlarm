package com.simples.j.worldtimealarm

import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import com.simples.j.worldtimealarm.utils.WakeUpService
import kotlinx.android.synthetic.main.activity_wake_up.*
import java.util.*

class WakeUpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var notificationManager: NotificationManager
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var sharedPref: SharedPreferences

    private var item: AlarmItem? = null
    private var actionBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up)

        if(!intent.hasExtra(AlarmReceiver.OPTIONS)) {
            Log.d(C.TAG, "Received Alarm came without information, Finish activity.")
            finish()
        }


        MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
        adViewWakeUp.loadAd(AdRequest.Builder().addTestDevice("6EF4925B538C754B535FCB7177FCAC3D").build())

        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dbCursor = DatabaseCursor(applicationContext)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        clock.format12Hour = MediaCursor.getLocalizedTimeFormat()
        clock_date.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")

        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        item = if(option != null && !option.isEmpty) option.getParcelable(AlarmReceiver.ITEM) else null

        item.let {
            Log.d(C.TAG, "Alarm alerted : ID(${it?.notiId?.plus(1)})")

            // Show selected time zone's time, but not print if time zone is default
            val timeZone = it?.timeZone?.replace(" ", "_")
            if(TimeZone.getDefault().id != TimeZone.getTimeZone(timeZone).id) {
                time_zone_clock_layout.visibility = View.VISIBLE
                val name = getNameForTimeZone(it?.timeZone)
                time_zone_clock_title.text = name

                time_zone_clock_time.timeZone = timeZone
                time_zone_clock_time.format12Hour = MediaCursor.getLocalizedTimeFormat()
                time_zone_clock_date.timeZone = timeZone
                time_zone_clock_date.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")
            }

            // Show label
            if(!it?.label.isNullOrEmpty()) {
                label.visibility = View.VISIBLE
                label.text = it?.label
                label.movementMethod = ScrollingMovementMethod()
            }
            if(it == null) {
                label.visibility = View.VISIBLE
                label.text = getString(R.string.error_message)
            }

            if(it?.snooze == null || it.snooze == 0L) snooze.visibility = View.GONE
        }

        snooze.setOnClickListener(this)
        dismiss.setOnClickListener(this)

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

        val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()

        stopService(Intent(applicationContext, WakeUpService::class.java))
        unregisterReceiver(actionBroadcastReceiver)
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
