package com.simples.j.worldtimealarm

import android.app.NotificationManager
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.receiver.AlarmReceiver
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
    private var isMenuExpanded = false
    private var actionBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up)

        if(!intent.hasExtra(AlarmReceiver.OPTIONS)) {
            Log.d(C.TAG, "Received Alarm came without information, Finish activity.")
            finish()
        }

        // fix floating action button bugs on version 28
        interaction_button.scaleType = ImageView.ScaleType.CENTER
        snooze.scaleType = ImageView.ScaleType.CENTER
        dismiss.scaleType = ImageView.ScaleType.CENTER

        MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
        adViewWakeUp.loadAd(AdRequest.Builder().addTestDevice("6EF4925B538C754B535FCB7177FCAC3D").build())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dbCursor = DatabaseCursor(applicationContext)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        clock_date.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")

        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        item = if(option != null && !option.isEmpty) option.getParcelable(AlarmReceiver.ITEM) else null

        item.let {
            Log.d(C.TAG, "Alarm alerted : ID(${it?.notiId?.plus(1)})")

            var color = it?.colorTag ?: 0
            if(color == 0) color = ContextCompat.getColor(applicationContext, R.color.blueGray)

            window.statusBarColor = color
            gradientEffect(color)

            val darken =
                    if(color == ContextCompat.getColor(applicationContext, android.R.color.black))
                        ContextCompat.getColor(applicationContext, R.color.darkerGray)
                    else convertColor(color, 0.75f)
            ViewCompat.setBackgroundTintList(interaction_button, ColorStateList.valueOf(darken))
            ViewCompat.setBackgroundTintList(dismiss, ColorStateList.valueOf(darken))
            ViewCompat.setBackgroundTintList(snooze, ColorStateList.valueOf(darken))

            // Show selected time zone's time, but not print if time zone is default
            val timeZone = it?.timeZone?.replace(" ", "_")
            if(!timeZone.isNullOrEmpty() && TimeZone.getDefault() != TimeZone.getTimeZone(timeZone)) {
                time_zone_clock_layout.visibility = View.VISIBLE
                val name = getNameForTimeZone(it.timeZone)
                time_zone_clock_title.text = name

                time_zone_clock_am_pm.timeZone = timeZone
                time_zone_clock_time.timeZone = timeZone
                time_zone_clock_date.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")
                time_zone_clock_date.timeZone = timeZone
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

            // if snooze is not set, interaction button will work like dismiss
            // ripple background won't start animation under xhdpi layout
            selector_layout?.startRippleAnimation()
            if(it?.snooze == 0L || intent.action == AlarmReceiver.ACTION_SNOOZE) {
                interaction_button.setImageDrawable(getDrawable(R.drawable.ic_action_alarm_off))
            }
        }

        interaction_button.setOnClickListener(this)
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

        notificationManager.cancel(item?.notiId ?: ALARM_NOTIFICATION_ID)

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
            R.id.interaction_button -> {
                if(item == null || item?.snooze == 0L || intent.action == AlarmReceiver.ACTION_SNOOZE) {
                    finish()
                }
                else {
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(selector)

                    if(!isMenuExpanded) {
                        constraintSet.connect(dismiss.id, ConstraintSet.START, interaction_button.id, ConstraintSet.END)
                        constraintSet.connect(snooze.id, ConstraintSet.END, interaction_button.id, ConstraintSet.START)
                        interaction_button.setImageDrawable(getDrawable(R.drawable.ic_action_close_white))
                    }
                    else {
                        constraintSet.connect(dismiss.id, ConstraintSet.START, selector.id, ConstraintSet.START)
                        constraintSet.connect(snooze.id, ConstraintSet.END, selector.id, ConstraintSet.END)
                        interaction_button.setImageDrawable(getDrawable(R.drawable.ic_action_menu_white))
                    }

                    val transition = AutoTransition()
                    transition.duration = 300
                    transition.interpolator = AccelerateDecelerateInterpolator()

                    TransitionManager.beginDelayedTransition(selector, transition)
                    constraintSet.applyTo(selector)

                    isMenuExpanded = !isMenuExpanded

                }
            }
            R.id.dismiss -> {
                finish()
            }
            R.id.snooze -> {
                AlarmController.getInstance().scheduleAlarm(applicationContext, item, AlarmController.TYPE_SNOOZE)

                val minutes = getString(R.string.minutes, item?.snooze?.div((60 * 1000)))
                Toast.makeText(applicationContext, getString(R.string.alarm_on, minutes), Toast.LENGTH_SHORT).show()
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

    private fun gradientEffect(color: Int) {

        val mid = convertColor(color, 0.5f)
        val end = convertColor(color, 1.5f)

        val gradient1 = GradientDrawable(GradientDrawable.Orientation.BR_TL, intArrayOf(color, mid, end))
        val gradient2 = GradientDrawable(GradientDrawable.Orientation.BR_TL, intArrayOf(end, color, mid))
        val gradient3 = GradientDrawable(GradientDrawable.Orientation.BR_TL, intArrayOf(mid, end, color))

        val animList = AnimationDrawable().apply {
            addFrame(gradient1, 500)
            addFrame(gradient2, 5000)
            addFrame(gradient3, 5000)
            addFrame(gradient1, 5000)
        }
        wake_up_layout.background = animList

        animList.setEnterFadeDuration(10)
        animList.setExitFadeDuration(5000)
        animList.start()
    }

    private fun convertColor(color: Int, value: Float): Int {
        val array = FloatArray(3)
        Color.colorToHSV(color, array)
        array[2] *= value

        return Color.HSVToColor(array)
    }

    companion object {
        const val ALARM_NOTIFICATION_ID = 0
        const val SHARED_ALARM_NOTIFICATION_ID = 132562

        const val ACTION_ACTIVITY_FINISH = "ACTION_ACTIVITY_FINISH"
    }

}
