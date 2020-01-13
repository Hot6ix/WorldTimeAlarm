package com.simples.j.worldtimealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.support.constraint.ConstraintSet
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.text.format.DateUtils
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
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.receiver.AlarmReceiver
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import com.simples.j.worldtimealarm.utils.VolumeController
import kotlinx.android.synthetic.main.activity_wake_up.*
import java.text.SimpleDateFormat
import java.util.*

class WakeUpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var powerManager: PowerManager
    private lateinit var wakeLocker: PowerManager.WakeLock
    private lateinit var notificationManager: NotificationManager
    private lateinit var dbCursor: DatabaseCursor
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPref: SharedPreferences
    private lateinit var timerHandler: Handler
    private lateinit var handlerRunnable: Runnable

    private var item: AlarmItem? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isMenuExpanded = false
    private var isExpired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up)
        isActivityRunning = true

        // fix floating action button bugs on version 28
        interaction_button.scaleType = ImageView.ScaleType.CENTER
        snooze.scaleType = ImageView.ScaleType.CENTER
        dismiss.scaleType = ImageView.ScaleType.CENTER

        MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
        adViewWakeUp.loadAd(AdRequest.Builder().addTestDevice("6EF4925B538C754B535FCB7177FCAC3D").build())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        dbCursor = DatabaseCursor(applicationContext)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        wakeLocker = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.WAKE_TAG)
        wakeLocker.acquire(AlarmReceiver.WAKE_LONG.toLong())

        clock_date.format12Hour = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MMM-d EEEE")

        val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        item = if(option != null && !option.isEmpty) option.getParcelable(AlarmReceiver.ITEM) else null
        isExpired = intent.getBooleanExtra(AlarmReceiver.EXPIRED, false)

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

            // Play ringtone
            val ringtoneUri = it?.ringtone
            val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
            if(ringtoneUri != null && ringtoneUri != "null") {
                player = MediaPlayer().apply {
                    setAudioAttributes(audioAttrs.build())
                    isLooping = true
                }

                try {
                    player?.setDataSource(applicationContext, Uri.parse(ringtoneUri))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()

                    // play default alarm sound if error occurred.
                    val sound = RingtoneManager.getActualDefaultRingtoneUri(applicationContext, RingtoneManager.TYPE_ALARM)
                    player?.setDataSource(applicationContext, sound)
                }
                player?.prepare()
                player?.start()

                if(sharedPref.getBoolean(resources.getString(R.string.setting_alarm_volume_increase_key), false)) {
                    val volume = VolumeController(applicationContext, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM))
                    volume.start()
                }
            }

            // Vibrate
            val vibrationPattern = it?.vibration
            if(vibrationPattern != null && vibrationPattern.isNotEmpty()) {
                vibrate(vibrationPattern)
            }

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

            // show notification
            showAlarmNotification(it, TYPE_ALARM)

            // if snooze is not set, interaction button will work like dismiss
            // ripple background won't start animation under xhdpi layout
            selector_layout?.startRippleAnimation()
            if(it?.snooze == 0L || intent.action == AlarmReceiver.ACTION_SNOOZE) {
                interaction_button.setImageDrawable(getDrawable(R.drawable.ic_action_alarm_off))
            }

            // Mute alarm sound if set
            timerHandler = Handler()
            handlerRunnable = Runnable {
                clear()
                Log.d(C.TAG, "Alarm muted : ID(${item?.notiId?.plus(1)})")
            }

            if(sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0")?.toInt() != 0) {
                timerHandler.postDelayed(handlerRunnable, sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0")!!.toLong())
            }
        }

        interaction_button.setOnClickListener(this)
        snooze.setOnClickListener(this)
        dismiss.setOnClickListener(this)
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(handlerRunnable)
        clear()
        player?.release()
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        isActivityRunning = false

        if(isExpired) {
            showAlarmNotification(item, TYPE_EXPIRED)
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Toast.makeText(applicationContext, getString(R.string.alarm_dismissed), Toast.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.interaction_button -> {
                if(item == null || item?.snooze == 0.toLong() || intent.action == AlarmReceiver.ACTION_SNOOZE) {
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

    private fun clear() {
        // Clear ringtone, vibrator, notification
        if(player != null && player!!.isPlaying) {
            player!!.stop()
        }
        if(vibrator != null && vibrator!!.hasVibrator()) vibrator!!.cancel()
    }

    private fun vibrate(array: LongArray?) {
        if(array != null) {
            if(Build.VERSION.SDK_INT < 26) {
                if(array.size > 1) vibrator?.vibrate(array, 0)
                else vibrator?.vibrate(array[0])
            }
            else {
                if(array.size > 1) vibrator?.vibrate(VibrationEffect.createWaveform(array, 0))
                else vibrator?.vibrate(VibrationEffect.createOneShot(array[0], VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun showAlarmNotification(item: AlarmItem?, type: Int) {

        val intent: Intent
        val title: String
        val notification = NotificationCompat.Builder(applicationContext, applicationContext.packageName)

        when(type) {
            TYPE_ALARM -> {
                intent = Intent(this, WakeUpActivity::class.java)

                title =
                    if(item != null) {
                        when {
                            isExpired && !item.label.isNullOrEmpty() -> {
                                resources.getString(R.string.last_alarm_with_time).format(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                            }
                            !item.label.isNullOrEmpty() -> {
                                SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))
                            }
                            isExpired -> {
                                resources.getString(R.string.last_alarm)
                            }
                            else -> {
                                resources.getString(R.string.alarm)
                            }
                        }
                    }
                    else {
                        resources.getString(R.string.error_occurred)
                    }


                val contentText =
                        if(item != null) {
                            if(!item.label.isNullOrEmpty()) {
                                item.label
                            }
                            else {
                                SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong()))
                            }
                        }
                        else {
                            resources.getString(R.string.error_message)
                        }

                notification
                        .setContentText(contentText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                        .setOngoing(true)
            }
            TYPE_EXPIRED -> {
                intent = Intent(this, MainActivity::class.java)
                if(item != null) {
                    intent.putExtra(AlarmListFragment.HIGHLIGHT_KEY, item.notiId)
                }

                title = if(item != null) {
                    getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(applicationContext, item.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME))
                }
                else resources.getString(R.string.error_occurred)

                notification
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .priority = NotificationCompat.PRIORITY_MAX
            }
            else -> {
                intent = Intent(this, MainActivity::class.java)
                title = "Wrong type of notification"
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(application.packageName, application.packageName+"/channel", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notification
                .setVibrate(LongArray(0))
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(this, if(item != null) item.notiId else SHARED_ALARM_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT))

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
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
        const val TYPE_ALARM = 100
        const val TYPE_EXPIRED = 101

        var isActivityRunning = false
    }

}
