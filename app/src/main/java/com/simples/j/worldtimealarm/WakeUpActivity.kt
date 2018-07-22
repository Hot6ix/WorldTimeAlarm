package com.simples.j.worldtimealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up)

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
        if(sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0").toInt() != 0) {
            timerHandler.postDelayed(handlerRunnable, sharedPref.getString(resources.getString(R.string.setting_alarm_mute_key), "0").toLong())
        }

        wakeLocker = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG)
        wakeLocker.acquire(AlarmReceiver.WAKE_LONG.toLong())

        isActivityRunning = true

        if(intent.extras != null) {
            val option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
            item = option.getParcelable(AlarmReceiver.ITEM)
//            Log.d(C.TAG, "Alarm alert : Info($item)")
            Log.d(C.TAG, "Alarm alerted : ID(${item.notiId+1})")

            // Change background color if color tag set
            if(item.colorTag != 0) {
                clock.setBackgroundColor(item.colorTag)
                label.setBackgroundColor(item.colorTag)
                window.statusBarColor = item.colorTag
            }

            // Play ringtone
            val ringtoneUri = item.ringtone
            val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
            if(ringtoneUri != null && ringtoneUri.isNotEmpty() && ringtoneUri != "null") {
                player = MediaPlayer()
                player?.setDataSource(applicationContext, Uri.parse(item.ringtone))
                player?.setAudioAttributes(audioAttrs.build())
                player?.isLooping = true
                player?.prepare()
                player?.start()

                if(sharedPref.getBoolean(resources.getString(R.string.setting_alarm_volume_increase_key), false)) {
                    val volume = VolumeController(applicationContext, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
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
                time_zone_name_wake.text = item.timeZone
                time_zone_offset_wake.text = MediaCursor.getOffsetOfDifference(applicationContext, difference)
                time_zone_time_wake.timeZone = timeZone
            }

            // Show label
            if(item.label != null && item.label!!.isNotEmpty()) {
                label.visibility = View.VISIBLE
                label.text = item.label
                label.movementMethod = ScrollingMovementMethod()
            }

            // Set snooze
            if(item.snooze == 0.toLong() || intent.action == AlarmReceiver.ACTION_SNOOZE) {
                snooze.visibility = View.GONE
            }

            // show notification
            showAlarmNotification()

//            // If alarm type is snooze ignore, if not set alarm
//            if(intent.action == AlarmReceiver.ACTION_ALARM) {
//                val repeatValue = applicationContext.resources.getIntArray(R.array.day_of_week_values)
//                val repeat = item.repeat.mapIndexed { index, i -> if(i == 1) repeatValue[index] else 0 }.filter { it != 0 }
//                val today = Calendar.getInstance()
//                if(repeat.contains(today.get(Calendar.DAY_OF_WEEK)) || repeat.isEmpty()) {
//                    val calendar = Calendar.getInstance()
//                    calendar.time = Date(item.timeSet.toLong())
//                    if(repeat.isEmpty()) {
//                        // Disable one time alarm
//                        calendar.add(Calendar.DAY_OF_YEAR, 1)
//                        item.timeSet = calendar.timeInMillis.toString()
//                        item.on_off = 0
//                        dbCursor.updateAlarm(item)
//                    }
//                    else {
//                        // If alarm is repeating, set next alarm
//                        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
//                        val currentDayIndex = repeat.indexOf(currentDay)
//
//                        if(currentDayIndex == repeat.lastIndex) {
//                            calendar.add(Calendar.WEEK_OF_MONTH, 1)
//                            calendar.set(Calendar.DAY_OF_WEEK, repeat[0])
//                        }
//                        else calendar.set(Calendar.DAY_OF_WEEK, repeat[currentDayIndex + 1])
//
//                        item.timeSet = calendar.timeInMillis.toString()
//                        dbCursor.updateAlarm(item)
//
//                        AlarmController.getInstance(applicationContext).scheduleAlarm(applicationContext, item, AlarmController.TYPE_ALARM)
//                    }
//
//                    val requestIntent = Intent(AlarmReceiver.ACTION_UPDATE)
//                    val bundle = Bundle()
//                    bundle.putParcelable(AlarmReceiver.ITEM, item)
//                    requestIntent.putExtra(AlarmReceiver.OPTIONS, bundle)
//                    sendBroadcast(requestIntent)
//                }
//            }
        }

        snooze.setOnClickListener(this)
        dismiss.setOnClickListener(this)
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(handlerRunnable)
        clear()
        player?.release()
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        isActivityRunning = false
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Toast.makeText(applicationContext, "Alarm dismissed.", Toast.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when(view.id) {
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

    private fun showAlarmNotification() {
        val intent = Intent(this, WakeUpActivity::class.java)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(application.packageName, application.packageName+"/channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = LongArray(0)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = NotificationCompat.Builder(applicationContext, applicationContext.packageName)
                .setVibrate(LongArray(0))
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(resources.getString(R.string.alarm))
                .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)

        if(item.label != null && item.label!!.isNotEmpty()) {
            notification.setContentTitle(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(item.timeSet.toLong())))
            notification.setContentText(item.label)
            notification.setStyle(NotificationCompat.BigTextStyle().bigText(item.label))
        }
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
    }

    companion object {
        const val ALARM_NOTIFICATION_ID = 0
        var isActivityRunning = false
    }

}
