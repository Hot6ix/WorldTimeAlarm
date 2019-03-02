package com.simples.j.worldtimealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.support.constraint.ConstraintSet
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
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
    private lateinit var item: AlarmItem
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPref: SharedPreferences
    private lateinit var timerHandler: Handler
    private lateinit var handlerRunnable: Runnable

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
        adViewWakeUp.loadAd(AdRequest.Builder().build())

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
        timerHandler = Handler()
        handlerRunnable = Runnable {
            clear()
            Log.d(C.TAG, "Alarm muted : ID(${item.notiId+1})")
        }

        if(sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0")?.toInt() != 0) {
            timerHandler.postDelayed(handlerRunnable, sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0")!!.toLong())
        }

        wakeLocker = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.WAKE_TAG)
        wakeLocker.acquire(AlarmReceiver.WAKE_LONG.toLong())

        if(intent.extras != null) {
            val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
            item = option.getParcelable(AlarmReceiver.ITEM)!!
            isExpired = intent.getBooleanExtra(AlarmReceiver.EXPIRED, false)

            Log.d(C.TAG, "Alarm alerted : ID(${item.notiId+1})")

            if(item.colorTag != 0) {
                wake_up_layout.setBackgroundColor(item.colorTag)
                window.statusBarColor = item.colorTag

                val darken =
                        if(item.colorTag == ContextCompat.getColor(applicationContext, android.R.color.black))
                            ContextCompat.getColor(applicationContext, R.color.blueGray)
                        else MediaCursor.makeDarker(item.colorTag, 0.85f)
                ViewCompat.setBackgroundTintList(interaction_button, ColorStateList.valueOf(darken))
                ViewCompat.setBackgroundTintList(dismiss, ColorStateList.valueOf(darken))
                ViewCompat.setBackgroundTintList(snooze, ColorStateList.valueOf(darken))
            }
            else wake_up_layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.blueGray))

            // Play ringtone
            val ringtoneUri = item.ringtone
            val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
            if(ringtoneUri != null && ringtoneUri.isNotEmpty() && ringtoneUri != "null") {
                player = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(item.ringtone))
                    setAudioAttributes(audioAttrs.build())
                    isLooping = true
                }
                player?.prepare()
                player?.start()

                if(sharedPref.getBoolean(resources.getString(R.string.setting_alarm_volume_increase_key), false)) {
                    val volume = VolumeController(applicationContext, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM))
                    volume.start()
                }
            }

            // Vibrate
            val vibrationPattern = item.vibration
            if(vibrationPattern != null && vibrationPattern.isNotEmpty()) {
                vibrate(vibrationPattern)
            }

            // Show selected time zone's time, but not print if time zone is default
            val timeZone = item.timeZone.replace(" ", "_")
            if(TimeZone.getDefault() != TimeZone.getTimeZone(timeZone)) {
//                val difference = TimeZone.getTimeZone(timeZone).rawOffset - TimeZone.getDefault().rawOffset + TimeZone.getTimeZone(timeZone).dstSavings - TimeZone.getDefault().dstSavings
                val difference = TimeZone.getTimeZone(timeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
                time_zone_layout.visibility = View.VISIBLE
                val filteredTimeZone = item.timeZone.split("/").let {
                    if(it.size > 1) it[it.lastIndex]
                    else it[0]
                }.replace("_", " ")
                time_zone_name_wake.text = filteredTimeZone
                time_zone_offset_wake.text = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
                time_zone_time_wake_am_pm.timeZone = timeZone
                time_zone_time_wake.timeZone = timeZone
            }

            // Show label
            if(item.label != null && item.label!!.trim().isNotEmpty()) {
                label.visibility = View.VISIBLE
                label.text = item.label
                label.movementMethod = ScrollingMovementMethod()
            }

            // show notification
            showAlarmNotification(TYPE_ALARM)

            // if snooze is not set, interaction button will work like dismiss
            selector_layout.startRippleAnimation()
            if(item.snooze == 0L || intent.action == AlarmReceiver.ACTION_SNOOZE) {
                interaction_button.setImageDrawable(getDrawable(R.drawable.ic_action_alarm_off))
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

        if(isExpired) showAlarmNotification(TYPE_EXPIRED)
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Toast.makeText(applicationContext, getString(R.string.alarm_dismissed), Toast.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.interaction_button -> {
                if(item.snooze == 0.toLong() || intent.action == AlarmReceiver.ACTION_SNOOZE) {
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
                AlarmController.getInstance(applicationContext).scheduleAlarm(applicationContext, item, AlarmController.TYPE_SNOOZE)

                val minutes = getString(R.string.minutes, item.snooze / (60 * 1000))
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

    private fun showAlarmNotification(type: Int) {

        val intent: Intent
        val title: String
        val notification = NotificationCompat.Builder(applicationContext, applicationContext.packageName)

        when(type) {
            TYPE_ALARM -> {
                intent = Intent(this, WakeUpActivity::class.java)

                title = when {
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

                notification
                        .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                        .setOngoing(true)

                if(item.label != null && item.label!!.isNotEmpty()) {
                    notification.setContentText(item.label)
                    notification.setStyle(NotificationCompat.BigTextStyle().bigText(item.label))
                }
            }
            TYPE_EXPIRED -> {
                intent = Intent(this, MainActivity::class.java)

                title = getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(applicationContext, item.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME))

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
            val notificationChannel = NotificationChannel(application.packageName, application.packageName+"/channel", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notification
                .setVibrate(LongArray(0))
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
    }

    companion object {
        const val ALARM_NOTIFICATION_ID = 0
        const val TYPE_ALARM = 100
        const val TYPE_EXPIRED = 101
        var isActivityRunning = false
    }

}
