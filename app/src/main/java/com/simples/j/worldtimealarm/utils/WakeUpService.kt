package com.simples.j.worldtimealarm.utils

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.simples.j.worldtimealarm.AlarmReceiver
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.WakeUpActivity
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.C.Companion.ALARM_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.EXPIRED_NOTIFICATION_CHANNEL
import com.simples.j.worldtimealarm.etc.C.Companion.GROUP_EXPIRED
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.receiver.NotificationActionReceiver
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

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
    private var serviceAction: String? = null
    private var serviceActionReceiver: BroadcastReceiver? = null
    private var defaultRingtone: RingtoneItem? = null

    private var isExpired = false

    override fun onBind(intent: Intent): IBinder {
        throw Exception("Not implemented")
    }

    override fun onCreate() {
        super.onCreate()

        isWakeUpServiceRunning = true
        preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        serviceActionReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                serviceAction = intent?.getStringExtra(SERVICE_ACTION)
                stopSelf()
            }
        }

        val filter = IntentFilter(REQUEST_SERVICE_ACTION)
        registerReceiver(serviceActionReceiver, filter)

        with(MediaCursor.getRingtoneList(applicationContext)) {
            defaultRingtone =
                    if(size > 1) this[1]
                    else this[0]
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) {
            Log.d(C.TAG, "Empty Intent arrived. Stop service.")
            stopSelf()
        }
        
        intent?.let {
            option = it.getBundleExtra(AlarmReceiver.OPTIONS)
            isExpired = it.getBooleanExtra(AlarmReceiver.EXPIRED, false)
            serviceAction = it.getStringExtra(SERVICE_ACTION)

            option?.let { bundle ->
                item = bundle.getParcelable(AlarmReceiver.ITEM)

                if(item == null) {
                    Log.d(C.TAG, "Empty Intent arrived. Stop service.")
                    stopSelf()
                }

                item?.let { alarmItem ->
                    startForeground(alarmItem.notiId, getNotification(AlarmReceiver.TYPE_ALARM, alarmItem, intent))
                    setup(alarmItem)
                    play(alarmItem.ringtone)
                    vibrate(alarmItem.vibration)
                    currentAlarmItemId = alarmItem.notiId

                    val alarmMuteTime = preference.getString(applicationContext.resources.getString(R.string.setting_alarm_mute_key), "0")?.toLong()
                    if(alarmMuteTime != null && alarmMuteTime != 0L) {

                        Runnable {
                            stopAll()
                            Log.d(C.TAG, "Alarm muted : ID(${alarmItem.notiId.plus(1)})")
                        }.also {  runnable ->
                            timerRunnable = runnable

                            timer = Handler(Looper.getMainLooper()).apply {
                                postDelayed(runnable, alarmMuteTime)
                            }
                        }
                    }
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        isWakeUpServiceRunning = false
        stopAll()
        timerRunnable?.let { timer?.removeCallbacks(it) }

        item?.let {
            notificationManager.cancel(it.notiId)
            when {
                serviceAction == AlarmReceiver.ACTION_SNOOZE -> {
                    Log.d(C.TAG, "Destroy service without disable alarm")
                }
                it.isInstantAlarm() -> {
                    AlarmController.getInstance().disableAlarm(applicationContext, it)
                }
                isExpired -> {
                    AlarmController.getInstance().disableAlarm(applicationContext, it)
                    notificationManager.notify(it.notiId, getNotification(AlarmReceiver.TYPE_EXPIRED, it))
                }
                else -> {
                    // This statement is for repeating alarm that is not expired yet
                    val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE).apply {
                        val bundle = Bundle()
                        bundle.putParcelable(AlarmReceiver.ITEM, it)
                        putExtra(AlarmReceiver.OPTIONS, bundle)
                    }
                    sendBroadcast(requestIntent)
                }
            }
        }

        try {
            unregisterReceiver(serviceActionReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isWakeUpServiceRunning = false
    }

    private fun setup(item: AlarmItem) {
        if(!item.ringtone.isNullOrEmpty())
            audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if(item.vibration != null)
            vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun getNotification(type: Int, alarmItem: AlarmItem, intent: Intent? = null): Notification {
        val dstIntent: Intent
        val title: String
        val notificationBuilder = NotificationCompat.Builder(applicationContext, ALARM_NOTIFICATION_CHANNEL)

        val instant = Instant.ofEpochMilli(alarmItem.timeSet.toLong())
        val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(alarmItem.timeZone))

        when(type) {
            AlarmReceiver.TYPE_ALARM -> {
                dstIntent = Intent(applicationContext, WakeUpActivity::class.java).apply {
                    action = intent?.action
                    putExtra(AlarmReceiver.OPTIONS, option)
                    putExtra(AlarmReceiver.EXPIRED, isExpired)
                }

                title =
                        when {
                            isExpired && !alarmItem.label.isNullOrEmpty() -> {
                                applicationContext.resources.getString(R.string.last_alarm_with_time).format(zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                            }
                            !alarmItem.label.isNullOrEmpty() -> {
                                zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
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
                        else if(intent?.action == AlarmReceiver.ACTION_SNOOZE) {
                            ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                        }
                        else {
                            zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                        }

                val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, NotificationActionReceiver.ACTION_DISMISS)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(applicationContext, alarmItem.notiId+20, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                val dismissAction = NotificationCompat.Action(0, getString(R.string.dismiss), dismissPendingIntent)

                val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, NotificationActionReceiver.ACTION_SNOOZE)
                    putExtra(AlarmReceiver.ITEM, alarmItem)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(applicationContext, alarmItem.notiId+30, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                val snoozeAction = NotificationCompat.Action(0, getString(R.string.snooze), snoozePendingIntent)

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(ALARM_NOTIFICATION_CHANNEL, getString(R.string.alarm_notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                        enableVibration(true)
                        vibrationPattern = LongArray(0)
                        importance = NotificationManager.IMPORTANCE_HIGH
                    }
                    notificationManager.createNotificationChannel(notificationChannel)
                }

                notificationBuilder
                        .addAction(dismissAction)
                        .setSound(null)
                        .setVibrate(null)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setContentText(contentText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                        .setFullScreenIntent(PendingIntent.getActivity(applicationContext, alarmItem.notiId, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT), true)

                if(alarmItem.snooze > 0 && intent?.action == AlarmReceiver.ACTION_ALARM) notificationBuilder.addAction(snoozeAction)
            }
            AlarmReceiver.TYPE_EXPIRED -> {
                dstIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(AlarmListFragment.HIGHLIGHT_KEY, alarmItem.notiId)
                }

                title = applicationContext.getString(R.string.alarm_no_long_fires).format(DateUtils.formatDateTime(applicationContext, alarmItem.timeSet.toLong(), DateUtils.FORMAT_SHOW_TIME))

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(EXPIRED_NOTIFICATION_CHANNEL, getString(R.string.expired_notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                        enableVibration(true)
                        vibrationPattern = LongArray(0)
                        importance = NotificationManager.IMPORTANCE_HIGH
                    }
                    notificationManager.createNotificationChannel(notificationChannel)
                }

                notificationBuilder
                        .setGroup(GROUP_EXPIRED)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(PendingIntent.getActivity(applicationContext, alarmItem.notiId+10, dstIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .priority = NotificationCompat.PRIORITY_HIGH
            }
            else -> {
                title = "Wrong type of notification"
            }
        }

        return notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.ic_action_alarm_white)
                .setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    private fun play(ringtone: String?) {
        if(ringtone.isNullOrEmpty() || ringtone == "null") return

        val audioAttrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttrs.build())
            isLooping = true
        }

        try {
            val ringtoneUri = Uri.parse(ringtone)
            mediaPlayer?.setDataSource(applicationContext, ringtoneUri)
        } catch (e: Exception) {
            e.printStackTrace()

            /*
                system caught exception when playing ringtone.
                now system will check whether the ringtone is valid or not.
                and if the ringtone is user-owned, but is not valid then will replace it to system default ringtone.
                also will play default ringtone.
            */
            if(!isSystemRingtone(ringtone)) {
                DatabaseCursor(applicationContext).findUserRingtone(ringtone).also {
                    if(it == null) {
                        // if ringtone is not in list, set to default ringtone
                        item?.also { item ->
                            item.ringtone = defaultRingtone?.uri
                            DatabaseCursor(applicationContext).updateAlarm(item)

                            // update alarm item in AlarmListFragment if fragment is attached and visible
                            val requestIntent = Intent(MainActivity.ACTION_UPDATE_SINGLE).apply {
                                val bundle = Bundle()
                                bundle.putParcelable(AlarmReceiver.ITEM, item)
                                putExtra(AlarmReceiver.OPTIONS, bundle)
                            }
                            sendBroadcast(requestIntent)
                        }
                    }
                }
            }

            val sound = try {
                Uri.parse(defaultRingtone?.uri)
            } catch (e: Exception) {
                RingtoneManager.getActualDefaultRingtoneUri(applicationContext, RingtoneManager.TYPE_ALARM)
            }

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
                @Suppress("DEPRECATION")
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
        // service can be stopped without playing ringtone or vibrate when item is null
        if(item != null) {
            mediaPlayer?.let {
                try {
                    if(it.isPlaying) it.stop()
                    it.release()
                    mediaPlayer = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            vibrator?.let {
                if(it.hasVibrator()) it.cancel()
            }
        }
    }

    private fun isSystemRingtone(uri: String): Boolean {
        val systemRingtoneList = MediaCursor.getRingtoneList(applicationContext)
        return systemRingtoneList.find { it.uri == uri } != null
    }

    companion object {
        var isWakeUpServiceRunning = false
        var currentAlarmItemId: Int? = -1

        const val REQUEST_SERVICE_ACTION = "REQUEST_SERVICE_ACTION"
        const val SERVICE_ACTION = "SERVICE_ACTION"
    }
}
