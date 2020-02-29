package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MenuItem
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.ACTION_REQUEST_AUDIO
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.ACTION_REQUEST_SNOOZE
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.ACTION_REQUEST_VIBRATION
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.AUDIO_REQUEST_CODE
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.LAST_SELECTED_KEY
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.SNOOZE_REQUEST_CODE
import com.simples.j.worldtimealarm.ContentSelectorActivity.Companion.VIBRATION_REQUEST_CODE
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.*
import com.simples.j.worldtimealarm.fragments.*
import com.simples.j.worldtimealarm.interfaces.OnDialogEventListener
import com.simples.j.worldtimealarm.support.AlarmDayAdapter
import com.simples.j.worldtimealarm.support.AlarmOptionAdapter
import com.simples.j.worldtimealarm.utils.AlarmController
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.activity_alarm.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class AlarmActivity : AppCompatActivity(), AlarmDayAdapter.OnItemClickListener, AlarmOptionAdapter.OnItemClickListener, View.OnClickListener, TimePicker.OnTimeChangedListener {

    private lateinit var alarmDayAdapter: AlarmDayAdapter
    private lateinit var alarmOptionAdapter: AlarmOptionAdapter
    private lateinit var ringtoneList: ArrayList<RingtoneItem>
    private lateinit var vibratorPatternList: ArrayList<PatternItem>
    private lateinit var snoozeList: ArrayList<SnoozeItem>
    private lateinit var optionList: ArrayList<OptionItem>
    private lateinit var selectedDays: IntArray

    private lateinit var calendar: Calendar
    private lateinit var currentTimeZone: String
    private lateinit var currentRingtone: RingtoneItem
    private lateinit var currentVibrationPattern: PatternItem
    private lateinit var currentSnooze: SnoozeItem
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager
    private lateinit var alarmController: AlarmController

    private lateinit var ringtoneDialog: ChoiceDialogFragment
    private lateinit var vibrationDialog: ChoiceDialogFragment
    private lateinit var snoozeDialog: SnoozeDialogFragment
    private lateinit var labelDialog: LabelDialogFragment
    private lateinit var colorTagDialog: ColorTagDialogFragment

    private lateinit var existAlarmItem: AlarmItem

    private var currentLabel: String? = null
    private var currentColorTag: Int = 0
    private var notiId = 0
    private var ringtone: Ringtone? = null
    private var alarmAction = -1
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var dateFormat = DateFormat.getDateInstance(DateFormat.FULL)

    private lateinit var prefManager: SharedPreferences
    private var timeZoneSelectorOption: String = ""
    private var applyDayRepetition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        supportActionBar?.apply {
            title = getString(R.string.new_alarm_short)
            setDisplayHomeAsUpEnabled(true)
        }

        alarmController = AlarmController.getInstance()

        prefManager = PreferenceManager.getDefaultSharedPreferences(this)
        timeZoneSelectorOption = prefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD
        applyDayRepetition = prefManager.getBoolean(getString(R.string.setting_time_zone_affect_repetition_key), false)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val userRingtone = DatabaseCursor(applicationContext).getUserRingtoneList()
        val systemRingtone = MediaCursor.getRingtoneList(applicationContext)
        val defaultRingtone = systemRingtone[1]

        ringtoneList = ArrayList<RingtoneItem>().apply {
            addAll(userRingtone)
            addAll(systemRingtone)
        }
        vibratorPatternList = MediaCursor.getVibratorPatterns(applicationContext)
        snoozeList = MediaCursor.getSnoozeList(applicationContext)

        val bundle = intent.getBundleExtra(BUNDLE_KEY)
        if(bundle != null) {
            // Modify
            alarmAction = ACTION_MODIFY

            bundle.getParcelable<AlarmItem>(AlarmReceiver.ITEM).let { alarmItem ->
                if(alarmItem == null) {
                    Toast.makeText(applicationContext, "Unable to get Alarm", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else {
                    existAlarmItem = alarmItem

                    currentTimeZone = alarmItem.timeZone.replace(" ", "_")

                    calendar = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                        timeInMillis = alarmItem.timeSet.toLong()
                        set(Calendar.SECOND, 0)
                    }

                    dateFormat.timeZone = TimeZone.getTimeZone(currentTimeZone)

                    if(Build.VERSION.SDK_INT < 23) {
                        time_picker.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        time_picker.currentMinute = calendar.get(Calendar.MINUTE)
                    }
                    else {
                        time_picker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                        time_picker.minute = calendar.get(Calendar.MINUTE)
                    }

                    time_zone.text = getNameForTimeZone(alarmItem.timeZone)
                    val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
                    var offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
                    if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                        offset = resources.getString(R.string.current_time_zone)
                        expectedTime.visibility = View.GONE
                    }
                    else {
                        expectedTime.visibility = View.VISIBLE
                        expectedTime.text = getString(R.string.expected_time,DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
                    }
                    time_zone_offset.text = offset

                    alarmItem.startDate?.let {
                        if(it > 0) {
                            Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                            }.let { startDateCalendar ->
                                startDate = startDateCalendar
                            }
                        }
                    }

                    alarmItem.endDate?.let {
                        if(it > 0) {
                            Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                            }.let { endDateCalendar ->
                                endDate = endDateCalendar
                            }
                        }
                    }

                    selectedDays = alarmItem.repeat
                    if(selectedDays.any { it == 1 }) {
                        // old way of day repeat
                        // convert to new
                        selectedDays = selectedDays.mapIndexed { index, i ->
                            if(i > 0) index + 1
                            else 0
                        }.toIntArray()
                    }

                    currentRingtone = ringtoneList.find { it.uri.toString() == alarmItem.ringtone } ?: defaultRingtone
                    currentVibrationPattern = vibratorPatternList.find { it.array?.contentEquals(alarmItem.vibration ?: LongArray(0)) ?: false } ?: vibratorPatternList[0]
                    currentSnooze = snoozeList.find { it.duration == alarmItem.snooze } ?: snoozeList[0]
                    currentLabel = alarmItem.label
                    currentColorTag = alarmItem.colorTag
                    optionList = getDefaultOptionList(
                            currentRingtone,
                            currentVibrationPattern,
                            currentSnooze,
                            currentLabel ?: "",
                            currentColorTag)
                }
            }
        }
        else {
            // New
            alarmAction = ACTION_NEW
            calendar = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
            }
            // If arrays don't contain default timezone id, add
            time_zone.text = getNameForTimeZone(TimeZone.getDefault().id)
            time_zone_offset.text = resources.getString(R.string.current_time_zone)
            selectedDays = IntArray(7) { 0 }
            optionList = getDefaultOptionList(defaultRingtone)
            currentTimeZone = TimeZone.getDefault().id
            expectedTime.visibility = View.GONE
        }

        // Restore data
        if(savedInstanceState != null) {
            currentTimeZone = savedInstanceState.getString(STATE_TIME_ZONE_KEY) ?: TimeZone.getDefault().id
            calendar = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone))
            calendar.time = savedInstanceState.getSerializable(STATE_DATE_KEY) as Date
            selectedDays = savedInstanceState.getIntArray(STATE_REPEAT_KEY) ?: IntArray(7) { 0 }
            dateFormat.timeZone = TimeZone.getTimeZone(currentTimeZone)

            if(Build.VERSION.SDK_INT < 23) {
                time_picker.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                time_picker.currentMinute = calendar.get(Calendar.MINUTE)
            }
            else {
                time_picker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                time_picker.minute = calendar.get(Calendar.MINUTE)
            }

            time_zone.text = getNameForTimeZone(currentTimeZone)
            val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
            var offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
            if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                offset = resources.getString(R.string.current_time_zone)
                expectedTime.visibility = View.GONE
            }
            else {
                expectedTime.visibility = View.VISIBLE
                expectedTime.text = getString(R.string.expected_time,DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
            }
            time_zone_offset.text = offset

            savedInstanceState.getLong(STATE_START_DATE_KEY).let {
                if(it > 0) {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                    }.let { startDateCalendar ->
                        startDate = startDateCalendar
                    }
                }
            }
            savedInstanceState.getLong(STATE_END_DATE_KEY).let {
                if(it > 0) {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                    }.let { endDateCalendar ->
                        endDate = endDateCalendar
                    }
                }
            }

            currentRingtone = savedInstanceState.getSerializable(STATE_RINGTONE_KEY) as RingtoneItem
            currentVibrationPattern = savedInstanceState.getSerializable(STATE_VIBRATION_KEY) as PatternItem
            currentSnooze = savedInstanceState.getSerializable(STATE_SNOOZE_KEY) as SnoozeItem
            currentLabel = savedInstanceState.getString(STATE_LABEL_KEY)
            currentColorTag = savedInstanceState.getInt(STATE_COLOR_TAG_KEY)

            optionList = getDefaultOptionList(
                    currentRingtone,
                    currentVibrationPattern,
                    currentSnooze,
                    currentLabel ?: "",
                    currentColorTag)
        }

        // init dialog
        ringtoneDialog = getRingtoneDialog()
        vibrationDialog = getVibrationDialog()
        snoozeDialog = getSnoozeDialog()
        labelDialog = getLabelDialog()
        colorTagDialog = getColorTagChoiceDialog()

        time_picker.setOnTimeChangedListener(this)
        time_zone_view.setOnClickListener(this)
        alarm_save.setOnClickListener(this)
        alarm_cancel.setOnClickListener(this)

        // init alarm repeat
        alarmDayAdapter = AlarmDayAdapter(selectedDays, applicationContext)
        alarmDayAdapter.setOnItemClickListener(this)
        repeat_day.layoutManager = GridLayoutManager(this, 7, GridLayoutManager.VERTICAL, false)
        repeat_day.adapter = alarmDayAdapter
        repeat_day.isNestedScrollingEnabled = false
        detail_content_layout.isNestedScrollingEnabled = false

        // init alarm options
        alarmOptionAdapter = AlarmOptionAdapter(optionList, applicationContext)
        alarmOptionAdapter.setOnItemClickListener(this)
        alarm_options.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        alarm_options.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
        alarm_options.adapter = alarmOptionAdapter
        alarm_options.isNestedScrollingEnabled = false
    }

    override fun onStop() {
        super.onStop()

        ringtone?.let {
            if(it.isPlaying) it.stop()
        }
        if(vibrator.hasVibrator()) vibrator.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)?.also {
                        currentTimeZone = it.replace(" ", "_")

                        with(TimeZone.getTimeZone(it)) {
                            calendar.timeZone = this
                            dateFormat.timeZone = this
//                        startDate?.timeZone = this
//                        endDate?.timeZone = this
                        }
                    }

                    val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())

                    val tmpCal = Calendar.getInstance().apply {
                        add(Calendar.MILLISECOND, difference)
                    }
                    if(tmpCal.after(startDate)) calendar = tmpCal

                    val offset: String
                    if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                        offset = resources.getString(R.string.current_time_zone)
                        expectedTime.visibility = View.GONE
                    }
                    else {
                        offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
                        val current = Calendar.getInstance()
                        calendar.apply {
                            set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.SECOND, 0)
                        }
                        expectedTime.visibility = View.VISIBLE
                        expectedTime.text = getString(R.string.expected_time, DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
                    }

                    time_zone.text = getNameForTimeZone(data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID))
                    time_zone_offset.text = offset

                }
            }
            requestCode == AUDIO_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.getSerializableExtra(LAST_SELECTED_KEY)?.also {
                    val ringtone = it as RingtoneItem
                    currentRingtone = ringtone
                    optionList[0].summary = ringtone.title
                    alarmOptionAdapter.notifyItemChanged(0)
                }
            }
            requestCode == VIBRATION_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.getSerializableExtra(LAST_SELECTED_KEY)?.also {
                    val vibration = it as PatternItem
                    currentVibrationPattern = vibration
                    optionList[1].summary = vibration.title
                    alarmOptionAdapter.notifyItemChanged(1)
                }
            }
            requestCode == SNOOZE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.getSerializableExtra(LAST_SELECTED_KEY)?.also {
                    val snooze = it as SnoozeItem
                    currentSnooze = snooze
                    optionList[2].summary = snooze.title
                    alarmOptionAdapter.notifyItemChanged(2)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TIME_ZONE_KEY, currentTimeZone)
        outState.putSerializable(STATE_DATE_KEY, calendar.time)
        outState.putIntArray(STATE_REPEAT_KEY, selectedDays)
        outState.putSerializable(STATE_RINGTONE_KEY, currentRingtone)
        outState.putSerializable(STATE_VIBRATION_KEY, currentVibrationPattern)
        outState.putSerializable(STATE_SNOOZE_KEY, currentSnooze)
        outState.putString(STATE_LABEL_KEY, currentLabel)
        outState.putInt(STATE_COLOR_TAG_KEY, currentColorTag)
        outState.putLong(STATE_START_DATE_KEY, startDate?.timeInMillis ?: 0)
        outState.putLong(STATE_END_DATE_KEY, endDate?.timeInMillis ?: 0)

        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }
            else -> false
        }
    }

    override fun onTimeChanged(picker: TimePicker?, hour: Int, minute: Int) {
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        expectedTime.text = getString(R.string.expected_time,DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))

        startDate?.set(Calendar.HOUR_OF_DAY, hour)
        startDate?.set(Calendar.MINUTE, minute)
        endDate?.set(Calendar.HOUR_OF_DAY, hour)
        endDate?.set(Calendar.MINUTE, minute)
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.alarm_save -> {

                val item = createAlarm()
                val start = startDate
                val end = endDate

                when {
                    start != null && end != null -> {
                        if(start.timeInMillis >= end.timeInMillis || end.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(applicationContext, getString(R.string.end_date_earlier_than_start_date), Toast.LENGTH_SHORT).show()
                            return
                        }

                        // alarm will be fired everyday if date range is less than a week.
                        val difference = end.timeInMillis - start.timeInMillis
                        when(TimeUnit.MILLISECONDS.toDays(difference)) {
                            in 1..6 -> {
                                val expect = try {
                                    alarmController.calculateDate(item, AlarmController.TYPE_ALARM, applyDayRepetition)
                                } catch (e: IllegalStateException) {
                                    null
                                }

                                if(expect == null || expect.timeInMillis < start.timeInMillis || expect.timeInMillis > end.timeInMillis) {
                                    Toast.makeText(applicationContext, getString(R.string.invalid_repeat), Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            else -> {
                                if(selectedDays.all { it == 0 }) {
                                    Toast.makeText(applicationContext, getString(R.string.must_check_repeat), Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                        }
                    }
                    start != null -> {
                        if(!selectedDays.any { it > 0 } && start.timeInMillis < System.currentTimeMillis()) {
                            Toast.makeText(applicationContext, getString(R.string.start_date_and_time_is_wrong), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                    end != null -> {

                        if(end.timeInMillis < System.currentTimeMillis()) {
                            Toast.makeText(applicationContext, getString(R.string.end_date_earlier_than_today), Toast.LENGTH_SHORT).show()
                            return
                        }

                        val difference = end.timeInMillis - System.currentTimeMillis()
                        when(TimeUnit.MILLISECONDS.toDays(difference)) {
                            in 1..6 -> {
                                val expect = try {
                                    alarmController.calculateDate(item, AlarmController.TYPE_ALARM, applyDayRepetition)
                                } catch (e: IllegalStateException) {
                                    null
                                }

                                if(expect == null || expect.timeInMillis <= System.currentTimeMillis() || expect.timeInMillis > end.timeInMillis) {
                                    Toast.makeText(applicationContext, getString(R.string.invalid_repeat), Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            else -> {
                                if(selectedDays.all { it == 0 }) {
                                    Toast.makeText(applicationContext, getString(R.string.must_check_repeat), Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                        }
                    }
                }

                val scheduledTime = alarmController.scheduleAlarm(this, item, AlarmController.TYPE_ALARM)
                if(scheduledTime == -1L) {
                    Toast.makeText(applicationContext, getString(R.string.unable_to_create_alarm), Toast.LENGTH_SHORT).show()
                    return
                }

                item.timeSet = scheduledTime.toString()

                if(alarmAction == ACTION_NEW) {
                    item.id = DatabaseCursor(applicationContext).insertAlarm(item).toInt()
                    item.index = item.id
                }
                else {
                    DatabaseCursor(applicationContext).updateAlarm(item)
                }

                if(isTaskRoot) showToast(scheduledTime)

                val intent = Intent()
                val bundle = Bundle().apply {
                    putParcelable(AlarmReceiver.ITEM, item)
                    putLong(SCHEDULED_TIME, scheduledTime)
                }
                intent.putExtra(AlarmReceiver.OPTIONS, bundle)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.alarm_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            R.id.time_zone_view -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && timeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(this, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_CHANGE)
                        putExtra(TimeZonePickerActivity.TIME_ZONE_ID, currentTimeZone)
                        putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_ALARM_CLOCK)
                    }
                    startActivityForResult(i, TIME_ZONE_REQUEST_CODE)
                }
                else startActivityForResult(Intent(this, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
        }
    }

    override fun onDayItemSelected(view: View, position: Int) {
        if(view.isSelected) selectedDays[position] = position + 1
        else selectedDays[position] = 0
    }

    override fun onItemClick(view: View, position: Int, item: OptionItem) {
        when(position) {
            0 -> { // Ringtone
                val contentIntent = Intent(this, ContentSelectorActivity::class.java).apply {
                    action = ACTION_REQUEST_AUDIO
                    putExtra(LAST_SELECTED_KEY, currentRingtone)
                }
                startActivityForResult(contentIntent, AUDIO_REQUEST_CODE)
//                if(!ringtoneDialog.isAdded) ringtoneDialog.show(supportFragmentManager, TAG_FRAGMENT_RINGTONE)
            }
            1 -> { // Vibration
                val contentIntent = Intent(this, ContentSelectorActivity::class.java).apply {
                    action = ACTION_REQUEST_VIBRATION
                    putExtra(LAST_SELECTED_KEY, currentVibrationPattern)
                }
                startActivityForResult(contentIntent, VIBRATION_REQUEST_CODE)
//                if(!vibrationDialog.isAdded) vibrationDialog.show(supportFragmentManager, TAG_FRAGMENT_VIBRATION)
            }
            2 -> { // Snooze
                val contentIntent = Intent(this, ContentSelectorActivity::class.java).apply {
                    action = ACTION_REQUEST_SNOOZE
                    putExtra(LAST_SELECTED_KEY, currentSnooze)
                }
                startActivityForResult(contentIntent, SNOOZE_REQUEST_CODE)
//                if(!snoozeDialog.isAdded) snoozeDialog.show(supportFragmentManager, TAG_FRAGMENT_SNOOZE)
            }
            3 -> { // Label
                if(!labelDialog.isAdded) labelDialog.show(supportFragmentManager, TAG_FRAGMENT_LABEL)
            }
            4 -> { // Color Tag
                if(!colorTagDialog.isAdded) colorTagDialog.show(supportFragmentManager, TAG_FRAGMENT_COLOR_TAG)
            }
        }
    }

    private fun getDefaultOptionList(defaultRingtone: RingtoneItem = ringtoneList[0],
                                     defaultVibration: PatternItem = vibratorPatternList[0],
                                     defaultSnooze: SnoozeItem = snoozeList[0],
                                     label: String = "",
                                     colorTag: Int = 0): ArrayList<OptionItem> {
        val array = ArrayList<OptionItem>()
        val options = resources.getStringArray(R.array.alarm_options)
        currentRingtone = defaultRingtone
        currentVibrationPattern = defaultVibration
        currentSnooze = defaultSnooze

        val values = arrayOf(currentRingtone.title, currentVibrationPattern.title, currentSnooze.title, label, colorTag.toString())
        options.forEachIndexed { index, s ->
            array.add(OptionItem(s, values[index]))
        }

        return array
    }

    private fun getRingtoneDialog(): ChoiceDialogFragment {
        val array = ringtoneList.map { it.title }.toTypedArray()
        var selected = ringtoneList.indexOf(currentRingtone)

        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_RINGTONE) as? ChoiceDialogFragment
        if(dialog == null) dialog = ChoiceDialogFragment.newInstance(resources.getString(R.string.select_ringtone), array)
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelect(inter: DialogInterface?, index: Int) {
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

            override fun onPositiveButtonClick(inter: DialogInterface, index: Int) {
                currentRingtone = ringtoneList[index]
                optionList[0].summary = ringtoneList[index].title
                alarmOptionAdapter.notifyItemChanged(0)
                ringtoneDialog.setLastChoice(index)
            }

            override fun onNegativeButtonClick(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onDialogDismiss(inter: DialogInterface?) {
                if(ringtone != null && ringtone!!.isPlaying) ringtone?.stop()
            }
        })

        return dialog
    }

    private fun getVibrationDialog(): ChoiceDialogFragment {
        val array = vibratorPatternList.map { it.title }.toTypedArray()
        var selected = vibratorPatternList.indexOf(currentVibrationPattern)

        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_VIBRATION) as? ChoiceDialogFragment
        if(dialog == null) dialog = ChoiceDialogFragment.newInstance(resources.getString(R.string.select_vibration), array)
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelect(inter: DialogInterface?, index: Int) {
                selected = index
                vibrator.cancel()
                if(index != 0) vibrate(vibratorPatternList[index].array)
            }

            override fun onPositiveButtonClick(inter: DialogInterface, index: Int) {
                currentVibrationPattern = vibratorPatternList[index]
                optionList[1].summary = vibratorPatternList[index].title
                alarmOptionAdapter.notifyItemChanged(1)
                vibrationDialog.setLastChoice(index)
            }

            override fun onNegativeButtonClick(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onDialogDismiss(inter: DialogInterface?) {
                if(vibrator.hasVibrator()) vibrator.cancel()
            }
        })

        return dialog
    }

    private fun getSnoozeDialog(): SnoozeDialogFragment {
        var selected = snoozeList.indexOf(currentSnooze)

        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_SNOOZE) as? SnoozeDialogFragment
        if(dialog == null) dialog = SnoozeDialogFragment.newInstance()
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelect(inter: DialogInterface?, index: Int) {
                selected = index
            }

            override fun onPositiveButtonClick(inter: DialogInterface, index: Int) {
                currentSnooze = snoozeList[index]
                optionList[2].summary = snoozeList[index].title
                alarmOptionAdapter.notifyItemChanged(2)
                snoozeDialog.setLastChoice(index)
            }

            override fun onNegativeButtonClick(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onDialogDismiss(inter: DialogInterface?) {}

        })
        return dialog
    }

    private fun getLabelDialog(): LabelDialogFragment {
        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_LABEL) as? LabelDialogFragment
        if(dialog == null) dialog = LabelDialogFragment.newInstance()
        if(!currentLabel.isNullOrEmpty()) {
            dialog.setLastLabel(currentLabel!!)
        }
        dialog.setOnDialogEventListener(object: LabelDialogFragment.OnDialogEventListener {
            override fun onPositiveButtonClick(inter: DialogInterface, label: String) {
                currentLabel = label
                optionList[3].summary = currentLabel ?: ""
                alarmOptionAdapter.notifyItemChanged(3)
                dialog.setLastLabel(label)
            }

            override fun onNegativeButtonClick(inter: DialogInterface) { inter.cancel() }

            override fun onNeutralButtonClick(inter: DialogInterface) {
                currentLabel = ""
                optionList[3].summary = ""
                alarmOptionAdapter.notifyItemChanged(3)
                dialog.setLastLabel("")
            }
        })
        return dialog
    }

    private fun getColorTagChoiceDialog(): ColorTagDialogFragment {
        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_COLOR_TAG) as? ColorTagDialogFragment
        if(dialog == null) dialog = ColorTagDialogFragment.newInstance()
        dialog.setLastChoice(currentColorTag)
        dialog.setOnDialogEventListener(object: ColorTagDialogFragment.OnDialogEventListener {
            override fun onPositiveButtonClick(inter: DialogInterface, color: Int) {
                currentColorTag = color
                optionList[4].summary = color.toString()
                alarmOptionAdapter.notifyItemChanged(4)
                dialog.setLastChoice(color)
            }

            override fun onNegativeButtonClick(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onNeutralButtonClick(inter: DialogInterface, index: Int) {
                currentColorTag = 0
                optionList[4].summary = currentColorTag.toString()
                alarmOptionAdapter.notifyItemChanged(4)
                dialog.setLastChoice(0)
            }

        })
        return dialog
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

    private fun createAlarm(): AlarmItem {
        val current = Calendar.getInstance()
        calendar.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.SECOND, 0)

        notiId = 100000 + Random().nextInt(899999)

        return AlarmItem(
                if(alarmAction == ACTION_NEW) null else existAlarmItem.id,
                currentTimeZone,
                calendar.time.time.toString(),
                selectedDays,
                currentRingtone.uri.toString(),
                currentVibrationPattern.array,
                currentSnooze.duration,
                optionList[3].summary,
                1,
                if(alarmAction == ACTION_NEW) notiId else existAlarmItem.notiId,
                currentColorTag,
                if(alarmAction == ACTION_NEW) -1 else existAlarmItem.index,
                startDate?.timeInMillis,
                endDate?.timeInMillis
        )
    }

    private fun showToast(scheduledTime: Long = -1) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = scheduledTime
        }
        Toast.makeText(applicationContext, getString(R.string.alarm_on, MediaCursor.getRemainTime(applicationContext, calendar)), Toast.LENGTH_LONG).show()
    }

    private fun getNameForTimeZone(timeZoneId: String?): String {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(timeZoneId))
        }
        else timeZoneId ?: getString(R.string.time_zone_unknown)
    }

    companion object {
        private const val STATE_TIME_ZONE_KEY = "STATE_TIME_ZONE_KEY"
        private const val STATE_DATE_KEY = "STATE_DATE_KEY"
        private const val STATE_REPEAT_KEY = "STATE_REPEAT_KEY"
        private const val STATE_RINGTONE_KEY = "STATE_RINGTONE_KEY"
        private const val STATE_VIBRATION_KEY = "STATE_VIBRATION_KEY"
        private const val STATE_SNOOZE_KEY = "STATE_SNOOZE_KEY"
        private const val STATE_LABEL_KEY = "STATE_LABEL_KEY"
        private const val STATE_COLOR_TAG_KEY = "STATE_COLOR_TAG_KEY"
        private const val STATE_START_DATE_KEY = "STATE_START_DATE_KEY"
        private const val STATE_END_DATE_KEY = "STATE_END_DATE_KEY"

        private const val TAG_FRAGMENT_RINGTONE = "TAG_FRAGMENT_RINGTONE"
        private const val TAG_FRAGMENT_VIBRATION = "TAG_FRAGMENT_VIBRATION"
        private const val TAG_FRAGMENT_SNOOZE = "TAG_FRAGMENT_SNOOZE"
        private const val TAG_FRAGMENT_LABEL = "TAG_FRAGMENT_LABEL"
        private const val TAG_FRAGMENT_COLOR_TAG = "TAG_FRAGMENT_COLOR_TAG"

        private const val ACTION_NEW = 0
        private const val ACTION_MODIFY = 1

        const val BUNDLE_KEY = "BUNDLE_KEY"
        const val SCHEDULED_TIME = "SCHEDULED_TIME"
    }
}
