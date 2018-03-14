package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.*
import com.simples.j.worldtimealarm.fragments.ChoiceDialogFragment
import com.simples.j.worldtimealarm.interfaces.OnDialogEventListener
import com.simples.j.worldtimealarm.support.AlarmDayAdapter
import com.simples.j.worldtimealarm.support.AlarmOptionAdapter
import com.simples.j.worldtimealarm.utils.MediaCursor
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import kotlinx.android.synthetic.main.activity_alarm.*
import java.util.*
import kotlin.collections.ArrayList

class AlarmActivity : AppCompatActivity(), AlarmDayAdapter.OnItemClickListener, AlarmOptionAdapter.OnItemClickListener, View.OnClickListener, TimePicker.OnTimeChangedListener {

    private lateinit var alarmDayAdapter: AlarmDayAdapter
    private lateinit var alarmOptionAdapter: AlarmOptionAdapter
    private lateinit var ringtoneList: ArrayList<RingtoneItem>
    private lateinit var vibratorPatternList: ArrayList<PatternItem>
    private lateinit var snoozeTimeList: Array<String>
    private lateinit var snoozeValues: Array<Long>
    private lateinit var optionList: ArrayList<OptionItem>

    private lateinit var calendar: Calendar
    private lateinit var currentTimeZone: String
    private lateinit var currentRingtone: RingtoneItem
    private lateinit var currentVibrationPattern: PatternItem
    private lateinit var ringtoneDialog: ChoiceDialogFragment
    private lateinit var vibrationDialog: ChoiceDialogFragment
    private lateinit var snoozeDialog: AlertDialog
    private lateinit var labelDialog: AlertDialog
    private lateinit var selectedDays: IntArray

    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager

    private var currentSnooze: Long = 0
    private var currentLabel: String? = null
    private var notiId = 0
    private var isNew = true
    private lateinit var snoozeSeekBar: SeekBar
    private var existAlarmItem: AlarmItem? = null
    private var labelEditor: EditText? = null
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
        adViewAlarm.loadAd(AdRequest.Builder().build())

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        ringtoneList = MediaCursor.getRingtoneList(applicationContext)
        vibratorPatternList = MediaCursor.getVibratorPatterns(applicationContext)
        snoozeTimeList = resources.getStringArray(R.array.snooze_array)
        snoozeValues = resources.getIntArray(R.array.snooze_values).map { it.toLong() }.toTypedArray()

        if(intent.hasExtra(AlarmReceiver.ITEM)) {
            // Modify
            isNew = false
            existAlarmItem = intent.getParcelableExtra<AlarmItem>(AlarmReceiver.ITEM)
            currentTimeZone = existAlarmItem!!.timeZone
            calendar = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone))
            calendar.timeInMillis = existAlarmItem!!.timeSet.toLong()
            calendar.set(Calendar.SECOND, 0)

            if(Build.VERSION.SDK_INT < 23) {
                time_picker.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                time_picker.currentMinute = calendar.get(Calendar.MINUTE)
            }
            else {
                time_picker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                time_picker.minute = calendar.get(Calendar.MINUTE)
            }

            time_zone.text = existAlarmItem!!.timeZone
            var offset = MediaCursor.getOffsetOfDifference(applicationContext,TimeZone.getTimeZone(existAlarmItem!!.timeZone).rawOffset - TimeZone.getDefault().rawOffset)
            if(TimeZone.getDefault() == TimeZone.getTimeZone(existAlarmItem!!.timeZone)) offset = resources.getString(R.string.current_time_zone)
            time_zone_offset.text = offset
            selectedDays = existAlarmItem!!.repeat

            currentRingtone = ringtoneList.find { it.uri.toString() == existAlarmItem!!.ringtone } ?: ringtoneList[0]
            currentVibrationPattern = vibratorPatternList.find { it.array?.contentEquals(existAlarmItem!!.vibration ?: LongArray(0)) ?: false } ?: vibratorPatternList[0]
            currentSnooze = snoozeValues.find { it == existAlarmItem!!.snooze } ?: snoozeValues[0]
            currentLabel = existAlarmItem!!.label
            optionList = getDefaultOptionList(
                     currentRingtone,
                    currentVibrationPattern,
                    currentSnooze,
                    currentLabel ?: "")
        }
        else {
            // New
            calendar = Calendar.getInstance()
            // If arrays don't contain default timezone id, add
            time_zone.text = TimeZone.getDefault().id
            time_zone_offset.text = resources.getString(R.string.current_time_zone)
            selectedDays = intArrayOf(0, 0, 0, 0, 0, 0, 0)
            optionList = getDefaultOptionList()
            currentTimeZone = TimeZone.getDefault().id
        }

        ringtoneDialog = getRingtoneDialog()
        vibrationDialog = getVibrationDialog()
        snoozeDialog = getSnoozeDialog()
        labelDialog = getLabelDialog()

        time_picker.setOnTimeChangedListener(this)
        time_zone_view.setOnClickListener(this)
        alarm_save.setOnClickListener(this)
        alarm_cancel.setOnClickListener(this)

        alarmDayAdapter = AlarmDayAdapter(selectedDays, applicationContext)
        alarmDayAdapter.setOnItemClickListener(this)
        repeat_day.layoutManager = GridLayoutManager(this, 7, GridLayoutManager.VERTICAL, false)
        repeat_day.adapter = alarmDayAdapter
        repeat_day.isNestedScrollingEnabled = false
        detail_content_layout.isNestedScrollingEnabled = false

        alarmOptionAdapter = AlarmOptionAdapter(optionList)
        alarmOptionAdapter.setOnItemClickListener(this)
        alarm_options.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        alarm_options.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
        alarm_options.adapter = alarmOptionAdapter
        alarm_options.isNestedScrollingEnabled = false
    }

    override fun onStop() {
        if(ringtone != null && ringtone!!.isPlaying) ringtone?.stop()
        if(vibrator.hasVibrator()) vibrator.cancel()

        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    currentTimeZone = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)

                    val difference = TimeZone.getTimeZone(currentTimeZone).rawOffset - TimeZone.getDefault().rawOffset
                    val offset = if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) resources.getString(R.string.current_time_zone)
                    else MediaCursor.getOffsetOfDifference(applicationContext, difference)

                    time_zone.text = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)
                    time_zone_offset.text = offset

                    calendar.timeZone = TimeZone.getTimeZone(currentTimeZone)
                }
            }
        }
    }

    override fun onTimeChanged(picker: TimePicker?, hour: Int, minute: Int) {
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.alarm_save -> {
                val item = scheduleAlarm()

                if(isNew) DatabaseCursor(applicationContext).insertAlarm(item)
                else DatabaseCursor(applicationContext).updateAlarm(item)

                val intent = Intent()
                intent.putExtra(AlarmReceiver.ITEM, item)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.alarm_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            R.id.time_zone_view -> {
                startActivityForResult(Intent(this, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
        }
    }

    override fun onItemClickListener(view: View, position: Int) {
        if(view.isSelected) selectedDays[position] = 1
        else selectedDays[position] = 0
    }

    override fun onItemClick(view: View, position: Int, item: OptionItem) {
        when(position) {
            0 -> { // Ringtone
                ringtoneDialog.show(fragmentManager, TAG_FRAGMENT_RINGTONE)
            }
            1 -> { // Vibration
                vibrationDialog.show(fragmentManager, TAG_FRAGMENT_VIBRATION)
            }
            2 -> { // Snoose
                snoozeDialog.show()
            }
            3 -> { // Label
                labelDialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                labelDialog.show()
            }
        }
    }

    private fun getDefaultOptionList(defaultRingtone: RingtoneItem = ringtoneList[0], defaultVibration: PatternItem = vibratorPatternList[0], defaultSnooze: Long = snoozeValues[0], label: String = ""): ArrayList<OptionItem> {
        val array = ArrayList<OptionItem>()
        val options = resources.getStringArray(R.array.alarm_options)
        currentRingtone = defaultRingtone
        currentVibrationPattern = defaultVibration
        currentSnooze = defaultSnooze

        val values = arrayOf(currentRingtone.title, currentVibrationPattern.name, snoozeTimeList[snoozeValues.indexOf(currentSnooze)], label)
        options.forEachIndexed { index, s ->
            array.add(OptionItem(s, values[index]))
        }

        return array
    }

    private fun getRingtoneDialog(): ChoiceDialogFragment {
        val array = ringtoneList.map { it.title }.toTypedArray()
        var selected = ringtoneList.indexOf(currentRingtone)

        var dialog = fragmentManager.findFragmentByTag(TAG_FRAGMENT_RINGTONE) as? ChoiceDialogFragment
        if(dialog == null) dialog = ChoiceDialogFragment.newInstance(resources.getString(R.string.select_ringtone), array)
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelected(inter: DialogInterface, index: Int) {
                selected = index
                if(selected != 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) * 60) / 100, 0)
                    val audioAttrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()

                    if(ringtone != null) ringtone?.stop()
                    ringtone = RingtoneManager.getRingtone(applicationContext, Uri.parse(ringtoneList[selected].uri))
                    ringtone?.audioAttributes = audioAttrs
                    ringtone?.play()
                }
                else ringtone?.stop()
            }

            override fun onPositiveButtonClickListener(inter: DialogInterface, index: Int) {
                currentRingtone = ringtoneList[selected]
                optionList[0].summary = ringtoneList[selected].title
                alarmOptionAdapter.notifyItemChanged(0)
                ringtoneDialog.setLastChoice(selected)
            }

            override fun onNegativeButtonClickListener(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onDialogDismissListener(inter: DialogInterface?) {
                if(ringtone != null && ringtone!!.isPlaying) ringtone?.stop()
            }
        })

        return dialog
    }

    private fun getVibrationDialog(): ChoiceDialogFragment {
        val array = vibratorPatternList.map { it.name }.toTypedArray()
        var selected = vibratorPatternList.indexOf(currentVibrationPattern)

        var dialog = fragmentManager.findFragmentByTag(TAG_FRAGMENT_VIBRATION) as? ChoiceDialogFragment
        if(dialog == null) dialog = ChoiceDialogFragment.newInstance(resources.getString(R.string.select_vibration), array)
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelected(inter: DialogInterface, index: Int) {
                selected = index
                vibrator.cancel()
                if(index != 0) vibrate(vibratorPatternList[index].array)
            }

            override fun onPositiveButtonClickListener(inter: DialogInterface, index: Int) {
                currentVibrationPattern = vibratorPatternList[selected]
                optionList[1].summary = vibratorPatternList[selected].name
                alarmOptionAdapter.notifyItemChanged(1)
                vibrationDialog.setLastChoice(selected)
            }

            override fun onNegativeButtonClickListener(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onDialogDismissListener(inter: DialogInterface?) {
                if(vibrator.hasVibrator()) vibrator.cancel()
            }
        })

        return dialog
    }

    private fun getSnoozeDialog(): AlertDialog {
        val snoozeView = View.inflate(applicationContext, R.layout.snooze_dialog_view, null)
        val snoozeTime = snoozeView.findViewById<TextView>(R.id.snooze_time)
        snoozeTime.text = optionList[2].summary
        snoozeSeekBar = snoozeView.findViewById(R.id.snooze)
        if(!isNew) snoozeSeekBar.progress = snoozeValues.indexOf(existAlarmItem!!.snooze)
        snoozeSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSnooze = snoozeValues[progress]
                snoozeTime.text = snoozeTimeList[progress]
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(this)
                .setView(snoozeView)
                .setPositiveButton(resources.getString(R.string.ok), { _, _ ->
                    optionList[2].summary = snoozeTime.text.toString()
                    alarmOptionAdapter.notifyItemChanged(2)
                })
                .setNegativeButton(resources.getString(R.string.cancel), { dialogInterface, _ ->
                    dialogInterface.cancel()
                })
                .setOnCancelListener { snoozeSeekBar.progress = snoozeTimeList.indexOf(optionList[2].summary) }
        return dialog.create()
    }

    private fun getLabelDialog(): AlertDialog {
        val labelView = View.inflate(applicationContext, R.layout.label_dialog_view, null)
        labelEditor = labelView.findViewById(R.id.label)
        if(currentLabel != null && currentLabel!!.isNotEmpty()) {
            labelEditor?.append(currentLabel)
        }
        val dialog = AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.label))
                .setView(labelView)
                .setPositiveButton(resources.getString(R.string.ok), { _, _ ->
                    currentLabel = labelEditor?.text.toString()
                    optionList[3].summary = currentLabel ?: ""
                    alarmOptionAdapter.notifyItemChanged(3)
                })
                .setNegativeButton(resources.getString(R.string.cancel), { dialogInterface, _ ->
                    dialogInterface.cancel()
                })
                .setOnCancelListener { labelEditor?.setText(optionList[3].summary) }
        return dialog.create()
    }

    private fun vibrate(array: LongArray?) {
        if(array != null) {
            if(Build.VERSION.SDK_INT < 26) {
                if(array.size > 1) vibrator.vibrate(array, -1)
                else vibrator.vibrate(array[0])
            }
            else {
                if(array.size > 1) vibrator.vibrate(VibrationEffect.createWaveform(array, -1))
                else vibrator.vibrate(VibrationEffect.createOneShot(array[0], VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun scheduleAlarm(): AlarmItem {
        val current = Calendar.getInstance()
        calendar.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.SECOND, 0)

        notiId = 100000 + Random().nextInt(899999)

        val item = AlarmItem(
                null,
                currentTimeZone,
                calendar.time.time.toString(),
                selectedDays,
                currentRingtone.uri.toString(),
                currentVibrationPattern.array,
                currentSnooze,
                optionList[3].summary,
                1,
                if(isNew) notiId else existAlarmItem!!.notiId
        )

        AlarmController.getInstance(this).scheduleAlarm(this, item, AlarmController.TYPE_ALARM)
        return item
    }

    companion object {
        private const val TAG_FRAGMENT_RINGTONE = "TAG_FRAGMENT_RINGTONE"
        private const val TAG_FRAGMENT_VIBRATION = "TAG_FRAGMENT_VIBRATION"
        private const val TIME_ZONE_REQUEST_CODE = 1
    }
}
