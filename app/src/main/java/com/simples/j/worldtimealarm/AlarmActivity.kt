package com.simples.j.worldtimealarm

import android.app.Activity
import android.app.DatePickerDialog
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
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.etc.OptionItem
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
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

class AlarmActivity : AppCompatActivity(), AlarmDayAdapter.OnItemClickListener, AlarmOptionAdapter.OnItemClickListener, View.OnClickListener, TimePicker.OnTimeChangedListener, View.OnLongClickListener {

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
    private lateinit var snoozeDialog: SnoozeDialogFragment
    private lateinit var labelDialog: LabelDialogFragment
    private lateinit var colorTagDialog: ColorTagDialogFragment
    private lateinit var startDatePickerDialog: DatePickerDialogFragment
    private lateinit var endDatePickerDialog: DatePickerDialogFragment
    private lateinit var selectedDays: IntArray

    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager

    private var currentSnooze: Long = 0
    private var currentLabel: String? = null
    private var currentColorTag: Int = 0
    private var notiId = 0
    private var isNew = true
    private var existAlarmItem: AlarmItem? = null
    private var ringtone: Ringtone? = null
    private var alarmAction = -1
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val today = Calendar.getInstance()
    private var dateFormat = DateFormat.getDateInstance(DateFormat.FULL)

    private lateinit var mPrefManager: SharedPreferences
    private var mTimeZoneSelectorOption: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        mPrefManager = PreferenceManager.getDefaultSharedPreferences(this)
        mTimeZoneSelectorOption = mPrefManager.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        ringtoneList = MediaCursor.getRingtoneList(applicationContext)
        vibratorPatternList = MediaCursor.getVibratorPatterns(applicationContext)
        snoozeTimeList = resources.getStringArray(R.array.snooze_array)
        snoozeValues = resources.getIntArray(R.array.snooze_values).map { it.toLong() }.toTypedArray()

        startDatePickerDialog =
                supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_START_DATE) as? DatePickerDialogFragment ?:
                DatePickerDialogFragment.newInstance().apply {
                    minDate = today.timeInMillis
                }
        startDatePickerDialog.setDateSetListener(startDatePickerListener)
        val tomorrow = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        endDatePickerDialog =
                supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_END_DATE) as? DatePickerDialogFragment ?:
                DatePickerDialogFragment.newInstance().apply {
                    setDate(tomorrow)
                    minDate = tomorrow.timeInMillis
                }
        endDatePickerDialog.setDateSetListener(endDatePickerListener)

        val bundle = intent.getBundleExtra(BUNDLE_KEY)
        if(bundle != null) {
            // Modify
            alarmAction = ACTION_MODIFY
            isNew = false
            existAlarmItem = bundle.getParcelable(AlarmReceiver.ITEM)
            currentTimeZone = existAlarmItem!!.timeZone.replace(" ", "_")

            calendar = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                timeInMillis = existAlarmItem!!.timeSet.toLong()
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

            time_zone.text = existAlarmItem!!.timeZone.replace("_", " ")
            val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
            var offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
            if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                offset = resources.getString(R.string.current_time_zone)
                expectedTime.visibility = View.GONE
                divider2.visibility = View.GONE
            }
            else {
                expectedTime.visibility = View.VISIBLE
                expectedTime.text = getString(R.string.expected_time,DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
                divider2.visibility = View.VISIBLE
            }
            time_zone_offset.text = offset

            val tmpCal = Calendar.getInstance().apply {
                add(Calendar.MILLISECOND, difference)
            }

            existAlarmItem?.startDate?.let {
                if(it > 0) {
                    startDate = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                        timeInMillis = it
                        set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                    }
                    startDatePickerDialog.calendar = startDate!!
                    startDatePickerDialog.minDate = tmpCal.timeInMillis

                    range_start.text = dateFormat.format(startDate?.time)
                }
            }

            existAlarmItem?.endDate?.let {
                if(it > 0) {
                    endDate = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone)).apply {
                        timeInMillis = it
                        set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                    }
                    endDatePickerDialog.calendar = endDate!!
                    tmpCal.add(Calendar.DAY_OF_YEAR, 1)
                    endDatePickerDialog.minDate = tmpCal.timeInMillis

                    range_end.text = dateFormat.format(endDate?.time)
                }
            }

            selectedDays = existAlarmItem!!.repeat
            if(selectedDays.any { it == 1 }) {
                // old way of day repeat
                // convert to new
                selectedDays = selectedDays.mapIndexed { index, i ->
                    if(i > 0) index + 1
                    else 0
                }.toIntArray()
            }

            currentRingtone = ringtoneList.find { it.uri.toString() == existAlarmItem!!.ringtone } ?: ringtoneList[1]
            currentVibrationPattern = vibratorPatternList.find { it.array?.contentEquals(existAlarmItem!!.vibration ?: LongArray(0)) ?: false } ?: vibratorPatternList[0]
            currentSnooze = snoozeValues.find { it == existAlarmItem!!.snooze } ?: snoozeValues[0]
            currentLabel = existAlarmItem!!.label
            currentColorTag = existAlarmItem!!.colorTag
            optionList = getDefaultOptionList(
                     currentRingtone,
                    currentVibrationPattern,
                    currentSnooze,
                    currentLabel ?: "",
                    currentColorTag)
        }
        else {
            // New
            alarmAction = ACTION_NEW
            calendar = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
            }
            // If arrays don't contain default timezone id, add
            time_zone.text = TimeZone.getDefault().id.replace("_", " ")
            time_zone_offset.text = resources.getString(R.string.current_time_zone)
            selectedDays = IntArray(7) { 0 }
            val defaultRingtone = if(ringtoneList.size > 1) ringtoneList[1] else ringtoneList[0]
            optionList = getDefaultOptionList(defaultRingtone)
            currentTimeZone = TimeZone.getDefault().id
            expectedTime.visibility = View.GONE
            divider2.visibility = View.GONE
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

            time_zone.text = currentTimeZone.replace("_", " ")
            val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())
            var offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
            if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                offset = resources.getString(R.string.current_time_zone)
                expectedTime.visibility = View.GONE
                divider2.visibility = View.GONE
            }
            else {
                expectedTime.visibility = View.VISIBLE
                expectedTime.text = getString(R.string.expected_time,DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
                divider2.visibility = View.VISIBLE
            }
            time_zone_offset.text = offset

            savedInstanceState.getLong(STATE_START_DATE_KEY).let {
                if(it > 0) {
                    startDate = Calendar.getInstance().apply {
                        timeInMillis = it
                    }
                    startDatePickerDialog.setDate(startDate!!)
                    range_start.text = dateFormat.format(startDate?.time)
                }
            }
            savedInstanceState.getLong(STATE_END_DATE_KEY).let {
                if(it > 0) {
                    endDate = Calendar.getInstance().apply {
                        timeInMillis = it
                    }
                    endDatePickerDialog.setDate(endDate!!)
                    range_end.text = dateFormat.format(endDate?.time)
                }
            }

            currentRingtone = savedInstanceState.getSerializable(STATE_RINGTONE_KEY) as RingtoneItem
            currentVibrationPattern = savedInstanceState.getSerializable(STATE_VIBRATION_KEY) as PatternItem
            currentSnooze = savedInstanceState.getLong(STATE_SNOOZE_KEY)
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
        alarm_options.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
        alarm_options.adapter = alarmOptionAdapter
        alarm_options.isNestedScrollingEnabled = false

        // init time range
        time_range_start.setOnClickListener(this)
        time_range_start.setOnLongClickListener(this)
        time_range_end.setOnClickListener(this)
        time_range_end.setOnLongClickListener(this)
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
                    currentTimeZone = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID).replace(" ", "_")

                    with(TimeZone.getTimeZone(currentTimeZone)) {
                        calendar.timeZone = this
                        dateFormat.timeZone = this
                        endDatePickerDialog.minDate = calendar.timeInMillis
//                        startDate?.timeZone = this
//                        endDate?.timeZone = this
                    }

                    val difference = TimeZone.getTimeZone(currentTimeZone).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis())

                    val tmpCal = Calendar.getInstance().apply {
                        add(Calendar.MILLISECOND, difference)
                    }
                    if(tmpCal.after(startDate)) calendar = tmpCal

                    // time can be yesterday or tomorrow, so apply new limit
                    startDatePickerDialog = DatePickerDialogFragment.newInstance().apply {
                        setDateSetListener(startDatePickerListener)
                        minDate = tmpCal.timeInMillis

                        startDate?.let {
                            calendar = it
                        }
                    }

                    val tmpNext = (tmpCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    endDatePickerDialog = DatePickerDialogFragment.newInstance().apply {
                        setDate(tmpNext)
                        setDateSetListener(endDatePickerListener)
                        minDate = tmpCal.timeInMillis

                        endDate?.let {
                            calendar = it
                        }
                    }

                    val offset: String
                    if(TimeZone.getDefault() == TimeZone.getTimeZone(currentTimeZone)) {
                        offset = resources.getString(R.string.current_time_zone)
                        expectedTime.visibility = View.GONE
                        divider2.visibility = View.GONE
                    }
                    else {
                        offset = MediaCursor.getOffsetOfDifference(applicationContext, difference, MediaCursor.TYPE_CURRENT)
                        val current = Calendar.getInstance()
                        calendar.apply {
                            set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.SECOND, 0)
                        }
                        expectedTime.visibility = View.VISIBLE
                        divider2.visibility = View.VISIBLE
                        expectedTime.text = getString(R.string.expected_time, DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time))
                    }

                    time_zone.text = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)
                    time_zone_offset.text = offset

                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(STATE_TIME_ZONE_KEY, currentTimeZone)
        outState?.putSerializable(STATE_DATE_KEY, calendar.time)
        outState?.putIntArray(STATE_REPEAT_KEY, selectedDays)
        outState?.putSerializable(STATE_RINGTONE_KEY, currentRingtone)
        outState?.putSerializable(STATE_VIBRATION_KEY, currentVibrationPattern)
        outState?.putLong(STATE_SNOOZE_KEY, currentSnooze)
        outState?.putString(STATE_LABEL_KEY, currentLabel)
        outState?.putInt(STATE_COLOR_TAG_KEY, currentColorTag)
        outState?.putLong(STATE_START_DATE_KEY, startDate?.timeInMillis ?: 0)
        outState?.putLong(STATE_END_DATE_KEY, endDate?.timeInMillis ?: 0)

        super.onSaveInstanceState(outState)
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
                if(startDate != null) {
                    when {
//                        startDate!!.timeInMillis < System.currentTimeMillis() -> {
//                            Toast.makeText(applicationContext, "No1!", Toast.LENGTH_SHORT).show()
//                            return
//                        }
                        !selectedDays.any { it > 0 } && endDate == null && startDate!!.timeInMillis < System.currentTimeMillis() -> {
                            Toast.makeText(applicationContext, getString(R.string.start_date_and_time_is_wrong), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                }
                if(endDate != null) {
                    when {
                        endDate!!.timeInMillis < System.currentTimeMillis() -> {
                            Toast.makeText(applicationContext, getString(R.string.end_date_earlier_than_today), Toast.LENGTH_SHORT).show()
                            return
                        }
                        !selectedDays.any { it > 0 } -> {
                            Toast.makeText(applicationContext, getString(R.string.must_check_repeat), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    val difference = endDate!!.timeInMillis - System.currentTimeMillis()
                    when(TimeUnit.MILLISECONDS.toDays(difference)) {
                        in 1..4 -> {
                            val copyOfRepeat = selectedDays.clone()
                            val tmpCal = today.clone() as Calendar
                            while(tmpCal.before(endDate)) {
                                val index = copyOfRepeat.indexOf(tmpCal.get(Calendar.DAY_OF_WEEK))
                                if(index > -1) copyOfRepeat[index] = 0
                                tmpCal.add(Calendar.DATE, 1)
                            }

                            if(copyOfRepeat.any { it > 0 }) {
                                Toast.makeText(applicationContext, getString(R.string.invalid_repeat), Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                    }
                }

                if(startDate != null && endDate != null) {
                    if(startDate!!.timeInMillis >= endDate!!.timeInMillis) {
                        Toast.makeText(applicationContext, getString(R.string.end_date_earlier_than_start_date), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // need to check repeat days that alarm makes sense
                    val difference = endDate!!.timeInMillis - startDate!!.timeInMillis
                    when(TimeUnit.MILLISECONDS.toDays(difference)) {
                        in 1..4 -> {
                            val copyOfRepeat = selectedDays.clone()
                            val tmpCal = startDate!!.clone() as Calendar
                            while(tmpCal.before(endDate)) {
                                val index = copyOfRepeat.indexOf(tmpCal.get(Calendar.DAY_OF_WEEK))
                                if(index > -1) copyOfRepeat[index] = 0
                                tmpCal.add(Calendar.DATE, 1)
                            }

                            if(copyOfRepeat.any { it > 0 }) {
                                Toast.makeText(applicationContext, getString(R.string.invalid_repeat), Toast.LENGTH_SHORT).show()
                                return
                            }
                            with(selectedDays.filter { it > 0 }) {
                                if(this.size == 1 && this[0] == startDate?.get(Calendar.DAY_OF_WEEK) && startDate!!.timeInMillis <= System.currentTimeMillis()) {
                                    Toast.makeText(applicationContext, getString(R.string.invalid_repeat), Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                        }
                    }
                }

                val item = createAlarm()

                if(isNew) {
                    item.id = DatabaseCursor(applicationContext).insertAlarm(item).toInt()
                    item.index = item.id
                }
                else {
                    DatabaseCursor(applicationContext).updateAlarm(item)
                }

                val scheduledTime = AlarmController.getInstance(this).scheduleAlarm(this, item, AlarmController.TYPE_ALARM)

                if(isTaskRoot) showToast(scheduledTime)

                val intent = Intent()
                intent.putExtra(AlarmReceiver.ITEM, item)
                intent.putExtra(SCHEDULED_TIME, scheduledTime)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.alarm_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            R.id.time_zone_view -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(this, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.TIME_ZONE_ID, currentTimeZone)
                    }
                    startActivityForResult(i, TIME_ZONE_REQUEST_CODE)
                }
                else startActivityForResult(Intent(this, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            }
            R.id.time_range_start -> {
                if(!startDatePickerDialog.isAdded) startDatePickerDialog.show(supportFragmentManager, TAG_FRAGMENT_START_DATE)
            }
            R.id.time_range_end -> {
                if(!endDatePickerDialog.isAdded) endDatePickerDialog.show(supportFragmentManager, TAG_FRAGMENT_END_DATE)
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when(view.id) {
            R.id.time_range_start -> {
                if(startDate != null) {
                    startDate = null
                    range_start.text = getString(R.string.range_not_set)
                    Toast.makeText(applicationContext, getString(R.string.start_date_removed),Toast.LENGTH_SHORT).show()
                }
            }
            R.id.time_range_end -> {
                if(endDate != null) {
                    endDate = null
                    range_end.text = getString(R.string.range_not_set)
                    Toast.makeText(applicationContext, getString(R.string.end_date_removed),Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    override fun onDayItemSelected(view: View, position: Int) {
        if(view.isSelected) selectedDays[position] = position + 1
        else selectedDays[position] = 0
    }

    override fun onItemClick(view: View, position: Int, item: OptionItem) {
        when(position) {
            0 -> { // Ringtone
                if(!ringtoneDialog.isAdded) ringtoneDialog.show(supportFragmentManager, TAG_FRAGMENT_RINGTONE)
            }
            1 -> { // Vibration
                if(!vibrationDialog.isAdded) vibrationDialog.show(supportFragmentManager, TAG_FRAGMENT_VIBRATION)
            }
            2 -> { // Snooze
                if(!snoozeDialog.isAdded) snoozeDialog.show(supportFragmentManager, TAG_FRAGMENT_SNOOZE)
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
                                     defaultSnooze: Long = snoozeValues[0],
                                     label: String = "",
                                     colorTag: Int = 0): ArrayList<OptionItem> {
        val array = ArrayList<OptionItem>()
        val options = resources.getStringArray(R.array.alarm_options)
        currentRingtone = defaultRingtone
        currentVibrationPattern = defaultVibration
        currentSnooze = defaultSnooze

        val values = arrayOf(currentRingtone.title, currentVibrationPattern.name, snoozeTimeList[snoozeValues.indexOf(currentSnooze)], label, colorTag.toString())
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
        val array = vibratorPatternList.map { it.name }.toTypedArray()
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
                optionList[1].summary = vibratorPatternList[index].name
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
        var selected = snoozeValues.indexOf(currentSnooze)

        var dialog = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_SNOOZE) as? SnoozeDialogFragment
        if(dialog == null) dialog = SnoozeDialogFragment.newInstance()
        dialog.setLastChoice(selected)
        dialog.setOnDialogEventListener(object: OnDialogEventListener {
            override fun onItemSelect(inter: DialogInterface?, index: Int) {
                selected = index
            }

            override fun onPositiveButtonClick(inter: DialogInterface, index: Int) {
                currentSnooze = snoozeValues[index]
                optionList[2].summary = snoozeTimeList[index]
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
                if(alarmAction == ACTION_NEW) null else existAlarmItem!!.id,
                currentTimeZone,
                calendar.time.time.toString(),
                selectedDays,
                currentRingtone.uri.toString(),
                currentVibrationPattern.array,
                currentSnooze,
                optionList[3].summary,
                1,
                if(isNew) notiId else existAlarmItem!!.notiId,
                currentColorTag,
                if(alarmAction == ACTION_NEW) -1 else existAlarmItem!!.index,
                startDate?.timeInMillis,
                endDate?.timeInMillis
        )
    }

    private fun showToast(scheduledTime: Long = -1) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = scheduledTime
        }
        Toast.makeText(applicationContext, getString(R.string.alarm_on, MediaCursor.getRemainTime(applicationContext, calendar)), Snackbar.LENGTH_LONG).show()
    }

    private val startDatePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        if(startDate == null) startDate = calendar.clone() as Calendar
        startDate?.set(year, month, dayOfMonth)
        startDatePickerDialog.calendar = startDate!!
        range_start.text = dateFormat.format(startDate?.time)
    }

    private val endDatePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        if(endDate == null) endDate = calendar.clone() as Calendar
        endDate?.set(year, month, dayOfMonth)
        endDatePickerDialog.calendar = endDate!!
        range_end.text = dateFormat.format(endDate?.time)
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
        private const val TAG_FRAGMENT_START_DATE = "TAG_FRAGMENT_START_DATE"
        private const val TAG_FRAGMENT_END_DATE = "TAG_FRAGMENT_END_DATE"

        private const val ACTION_NEW = 0
        private const val ACTION_MODIFY = 1

        const val BUNDLE_KEY = "BUNDLE_KEY"
        const val SCHEDULED_TIME = "SCHEDULED_TIME"
    }
}
