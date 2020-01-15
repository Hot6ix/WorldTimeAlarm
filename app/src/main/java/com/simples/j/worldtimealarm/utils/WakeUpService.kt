package com.simples.j.worldtimealarm.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.WakeUpActivity
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.receiver.AlarmReceiver
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

class WakeUpService : Service() {

    private lateinit var preference: SharedPreferences
    private lateinit var notificationManager: NotificationManager

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var option: Bundle? = null
    private var item: AlarmItem? = null
    private var timer: Handler? = null
    private var timerRunnable: Runnable? = null

    private var isExpired = false

    override fun onBind(intent: Intent): IBinder {
        throw Exception("Not implemented.")
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(C.TAG, "WakeUpService Started")
        preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) throw IllegalArgumentException("WakeUpService has empty intent.")

        option = intent.getBundleExtra(AlarmReceiver.OPTIONS)
        isExpired = intent.getBooleanExtra(AlarmReceiver.EXPIRED, false)

        option?.let {
            item = it.getParcelable(AlarmReceiver.ITEM)

            if(item == null) {
                Log.d(C.TAG, "Empty Intent arrived. Stop service.")
                stopService(intent)
            }

            item?.let { alarmItem ->
                if(!WakeUpActivity.isActivityRunning) {
                    startForeground(21342, getNotification(intent, AlarmReceiver.TYPE_ALARM, alarmItem))
                }
                else {
                    startForeground(21342, getNotification(intent, AlarmReceiver.TYPE_MISSED, alarmItem))
                }
                setup(alarmItem)
                play(alarmItem.ringtone)
                vibrate(alarmItem.vibration)

                val alarmMuteTime = preference.getString(applicationContext.resources.getString(R.string.setting_alarm_mute_key), "0")?.toLong()

                timerRunnable = Runnable {
                    stopAll()
                    Log.d(C.TAG, "Alarm muted : ID(${alarmItem.notiId.plus(1)})")
                }

                if(alarmMuteTime != null && alarmMuteTime != 0L) {
                    timer = Handler().apply {
                        postDelayed(timerRunnable, alarmMuteTime)
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
        timer?.removeCallbacks(timerRunnable)
    }

    private fun setup(item: AlarmItem) {
        if(!item.ringtone.isNullOrEmpty())
            audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if(item.vibration != null)
            vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun getNotification(intent: Intent, type: Int, alarmItem: AlarmItem): Notification {
        val dstIntent: Intent
        val title: String
        val notificationBuilder = NotificationCompat.Builder(applicationContext, AlarmReceiver.NOTIFICATION_CHANNEL_ID)

        when(type) {
            AlarmReceiver.TYPE_ALARM -> {
                dstIntent = Intent(applicationContext, WakeUpActivity::class.java).apply {
                    action = intent.action
                    putExtra(AlarmReceiver.OPTIONS, option)
                    putExtra(AlarmReceiver.EXPIRED, isExpired)
                }

                title =
                        when {
                            isExpired && !alarmItem.label.isNullOrEmpty() -> {
                                applicationContext.resources.getString(R.string.last_alarm_with_time).format(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(alarmItem.timeSet.toLong())))
                            }
                            !alarmItem.label.isNullOrEmpty() -> {
                                SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(alarmItem.timeSet.toLong()))
                            }
                            isExpired -> {
                                applicationContext.resources.getString(R.string.last_alarm)
                            }
                            else -> {
                                applicationContext.resources.getString(R.string.alarm)
                            }
                        }


                val contentText =
                        if(!alarmItem.label.isNullOrEmpty()) {
                            alarmItem.label
                        }
                        else {
                            SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(alarmItem.timeSet.toLong()))
                        }

                notificationBuilder
                        .setOngoing(true)
                        .setContentText(contentText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                        .setFullScreenIntent(PendingIntent.getActivity(applicationContext, alarmItem.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT), true)
            }
            AlarmReceiver.TYPE_EXPIRED -> {
                dstIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(AlarmListFragment.HIGHLIGHT_KEY, alarmItem.notiId)
                }

                title = applicationContext.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(applicationContext, alarmItem.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME))

                notificationBuilder
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setFullScreenIntent(PendingIntent.getActivity(applicationContext, alarmItem.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT), true)
                        .priority = NotificationCompat.PRIORITY_MAX
            }
            AlarmReceiver.TYPE_MISSED -> {
                dstIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(AlarmListFragment.HIGHLIGHT_KEY, alarmItem.notiId)
                }

                title = applicationContext.resources.getString(R.string.missed_alarm)

                notificationBuilder
                        .setContentText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(alarmItem.timeSet.toLong())))
                        .setContentIntent(PendingIntent.getActivity(applicationContext, alarmItem.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT))

                if(isExpired) {
                    notificationBuilder
                            .setAutoCancel(true)
                            .setContentTitle(applicationContext.resources.getString(R.string.missed_and_last_alarm))
                            .setContentText(applicationContext.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(applicationContext, alarmItem.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME)))
                            .setDefaults(Notification.DEFAULT_ALL)
                            .priority = NotificationCompat.PRIORITY_MAX
                }

                if(alarmItem.label != null && !alarmItem.label.isNullOrEmpty()) {
                    notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("${SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(alarmItem.timeSet.toLong()))} - ${alarmItem.label}"))
                }
            }
            else -> {
                title = "Wrong type of notification"
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(AlarmReceiver.NOTIFICATION_CHANNEL_ID, applicationContext.getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = LongArray(0)

            }
            notificationManager.createNotificationChannel(notificationChannel)
        }


        return notificationBuilder
                .setVibrate(LongArray(0))
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .build()
    }

    private fun play(ringtone: String?) {
        if(ringtone.isNullOrEmpty() || ringtone == "null") {
            Log.d(C.TAG, "Ringtone is empty.")
            return
        }

        val ringtoneUri = Uri.parse(ringtone)
        val audioAttrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)

        mediaPlayer = AlarmMediaPlayer.player.apply {
            setAudioAttributes(audioAttrs.build())
            isLooping = true
        }

        try {
            mediaPlayer?.setDataSource(applicationContext, ringtoneUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, applicationContext.getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()

            // play default alarm sound if error occurred.
            val sound = RingtoneManager.getActualDefaultRingtoneUri(applicationContext, RingtoneManager.TYPE_ALARM)
            mediaPlayer?.setDataSource(applicationContext, sound)
        }
        mediaPlayer?.prepare()
        mediaPlayer?.start()

        if(preference.getBoolean(applicationContext.resources.getString(R.string.setting_alarm_volume_increase_key), false)) {
            val volume = VolumeController(applicationContext)
            volume.start()
        }
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

    private fun stopAll() {
        mediaPlayer?.let {
            if(it.isPlaying) it.stop()
            it.release()
        }

        vibrator?.let {
            if(it.hasVibrator()) it.cancel()
        }
    }
}
