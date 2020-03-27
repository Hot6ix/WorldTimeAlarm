package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.TimePicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.etc.*
import com.simples.j.worldtimealarm.models.AlarmGeneratorViewModel
import com.simples.j.worldtimealarm.support.AlarmOptionAdapter
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.DstController
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.activity_alarm.action
import kotlinx.android.synthetic.main.activity_alarm.date
import kotlinx.android.synthetic.main.activity_alarm.day_recurrence
import kotlinx.android.synthetic.main.activity_alarm.time_zone
import kotlinx.android.synthetic.main.activity_alarm.time_zone_offset
import kotlinx.android.synthetic.main.fragment_alarm_generator.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.apply
import kotlin.collections.ArrayList

class AlarmGeneratorFragment : Fragment(), AlarmOptionAdapter.OnItemClickListener, View.OnClickListener, MaterialButtonToggleGroup.OnButtonCheckedListener, TimePicker.OnTimeChangedListener {

    private lateinit var fragmentContext: Context

    private lateinit var viewModel: AlarmGeneratorViewModel
    private lateinit var alarmOptionAdapter: AlarmOptionAdapter
    private lateinit var labelDialog: LabelDialogFragment
    private lateinit var colorTagDialog: ColorTagDialogFragment
    private lateinit var ringtoneList: ArrayList<RingtoneItem>
    private lateinit var vibratorPatternList: ArrayList<PatternItem>
    private lateinit var snoozeList: ArrayList<SnoozeItem>
    private lateinit var now: ZonedDateTime

    private lateinit var preference: SharedPreferences

    private var timeZoneSelectorOption: String = ""
    private val dateTimeChangedReceiver = DateTimeChangedReceiver()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alarm_generator, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)

        preference = PreferenceManager.getDefaultSharedPreferences(fragmentContext)
        timeZoneSelectorOption = preference.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD

        activity?.run {
            viewModel = ViewModelProvider(this)[AlarmGeneratorViewModel::class.java]
        }

        val dayIds = arrayOf(
                R.id.sunday,
                R.id.monday,
                R.id.tuesday,
                R.id.wednesday,
                R.id.thursday,
                R.id.friday,
                R.id.saturday
        )
        val recurrenceDays = fragmentContext.resources.getStringArray(R.array.day_of_week)

        val userRingtone = DatabaseCursor(fragmentContext).getUserRingtoneList()
        val systemRingtone = MediaCursor.getRingtoneList(fragmentContext)
        val defaultRingtone = systemRingtone[1]

        ringtoneList = ArrayList<RingtoneItem>().apply {
            addAll(userRingtone)
            addAll(systemRingtone)
        }
        vibratorPatternList = MediaCursor.getVibratorPatterns(fragmentContext)
        snoozeList = MediaCursor.getSnoozeList(fragmentContext)

        if(savedInstanceState == null) {
            viewModel.alarmItem.let {
                if(it != null) {
                    viewModel.timeZone.value = it.timeZone.replace(" ", "_")
                    now = ZonedDateTime.now(ZoneId.of(viewModel.timeZone.value))

                    val instant = Instant.ofEpochMilli(it.pickerTime)
                    val local = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
                    viewModel.remoteZonedDateTime = local.withZoneSameInstant(ZoneId.of(it.timeZone))

                    it.startDate?.let { startDateInMillis ->
                        if(startDateInMillis > 0) {
                            val startInstant = Instant.ofEpochMilli(startDateInMillis)
                            viewModel.startDate = ZonedDateTime.ofInstant(startInstant, ZoneId.of(viewModel.timeZone.value))
                                    .withHour(viewModel.remoteZonedDateTime.hour)
                                    .withMinute(viewModel.remoteZonedDateTime.minute)
                        }
                    }

                    it.endDate?.let { endDateInMillis ->
                        if(endDateInMillis > 0) {
                            val endInstant = Instant.ofEpochMilli(endDateInMillis)
                            viewModel.endDate = ZonedDateTime.ofInstant(endInstant, ZoneId.of(viewModel.timeZone.value))
                                    .withHour(viewModel.remoteZonedDateTime.hour)
                                    .withMinute(viewModel.remoteZonedDateTime.minute)
                        }
                    }

                    viewModel.startDate.let { start ->
                        if(start == null) {
                            viewModel.remoteZonedDateTime =
                                    viewModel.remoteZonedDateTime
                                            .withYear(now.year)
                                            .withMonth(now.monthValue)
                                            .withDayOfMonth(now.dayOfMonth)
                        }
                        else {
                            viewModel.remoteZonedDateTime =
                                    viewModel.remoteZonedDateTime
                                            .withYear(start.year)
                                            .withMonth(start.monthValue)
                                            .withDayOfMonth(start.dayOfMonth)
                        }
                    }

                    viewModel.recurrences.value = it.repeat
                    viewModel.recurrences.value?.let {  recurrences ->
                        if(recurrences.any { day -> day == 1 }) {
                            viewModel.recurrences.value = viewModel.recurrences.value?.mapIndexed { index, i ->
                                if(i > 0) index + 1
                                else 0
                            }?.toIntArray()
                        }
                    }

                    viewModel.ringtone.value = ringtoneList.find { item -> item.uri == it.ringtone } ?: defaultRingtone
                    viewModel.vibration.value = vibratorPatternList.find { item ->
                        if(item.array == null && it.vibration == null) true
                        else item.array?.contentEquals(it.vibration ?: longArrayOf(0)) ?: false
                    }
                    viewModel.snooze.value = snoozeList.find { item -> item.duration == it.snooze }
                    viewModel.label.value = it.label
                    viewModel.colorTag.value = it.colorTag
                }
                else {
                    viewModel.timeZone.value = TimeZone.getDefault().id
                    now = ZonedDateTime.now(ZoneId.systemDefault())

                    viewModel.ringtone.value = defaultRingtone
                    viewModel.vibration.value = vibratorPatternList[0]
                    viewModel.snooze.value = snoozeList[0]
                }
            }
        }

        val hour = viewModel.remoteZonedDateTime.hour
        val minute = viewModel.remoteZonedDateTime.minute
        if(Build.VERSION.SDK_INT < 23) {
            @Suppress("DEPRECATION")
            time_picker.currentHour = hour
            @Suppress("DEPRECATION")
            time_picker.currentMinute = minute
        }
        else {
            time_picker.hour = hour
            time_picker.minute = minute
        }

        time_zone.text = getFormattedTimeZoneName(viewModel.timeZone.value)
        updateTimeZone()

        action.apply {
            if(viewModel.alarmItem != null) {
                text = getString(R.string.apply)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_done_white)
            }
            else {
                text = getString(R.string.create)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_add)
            }
        }

        viewModel.recurrences.value?.let { recurrences ->
            recurrences.forEachIndexed { index, day ->
                if(day > 0) day_recurrence.check(dayIds[index])
            }
        }

        // init alarm options
        viewModel.optionList = getAlarmOptions()
        alarmOptionAdapter = AlarmOptionAdapter(viewModel.optionList, fragmentContext)
        alarmOptionAdapter.setOnItemClickListener(this)

        alarm_options.apply {
            adapter = alarmOptionAdapter
            layoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

            addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }

        date.text = formatDate()

        // init dialog
        labelDialog = getLabelDialog()
        colorTagDialog = getColorTagChoiceDialog()

        time_picker.setOnTimeChangedListener(this)
        time_zone_view.setOnClickListener(this)

        // init alarm recurrences
        recurrenceDays.forEachIndexed { index, s ->
            val button = day_recurrence.getChildAt(index) as MaterialButton
            button.text = s
            if(index == 0) button.setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_red_light))
            else if(index == 6) button.setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_blue_light))
        }

        day_recurrence.addOnButtonCheckedListener(this)
        date_view.setOnClickListener(this)
        action.setOnClickListener(this)
        detail_content_layout.isNestedScrollingEnabled = false

        // Init observers
        viewModel.ringtone.observe(viewLifecycleOwner, Observer {
            alarmOptionAdapter.notifyItemChanged(0)
        })
        viewModel.vibration.observe(viewLifecycleOwner, Observer {
            alarmOptionAdapter.notifyItemChanged(1)
        })
        viewModel.snooze.observe(viewLifecycleOwner, Observer {
            alarmOptionAdapter.notifyItemChanged(2)
        })
        viewModel.label.observe(viewLifecycleOwner, Observer {
            alarmOptionAdapter.notifyItemChanged(3)
        })
        viewModel.colorTag.observe(viewLifecycleOwner, Observer {
            alarmOptionAdapter.notifyItemChanged(4)
        })

        val intentFilter = IntentFilter(MainActivity.ACTION_UPDATE_ALL)
        fragmentContext.registerReceiver(dateTimeChangedReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentContext.unregisterReceiver(dateTimeChangedReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                TimeZoneSearchActivity.TIME_ZONE_REQUEST_CODE -> {
                    if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                        data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)?.also {
                            viewModel.timeZone.value = it.replace(" ", "_")

                            viewModel.remoteZonedDateTime =
                                    viewModel.remoteZonedDateTime
                                            .withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))

                            viewModel.startDate = viewModel.startDate?.withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))
                            viewModel.endDate = viewModel.endDate?.withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))
                        }

                        time_zone.text = getFormattedTimeZoneName(data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID))
                        updateTimeZone()
                    }
                }
                ContentSelectorActivity.AUDIO_REQUEST_CODE -> {
                    data?.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                        val ringtone = it as RingtoneItem
                        viewModel.ringtone.value = ringtone
                        viewModel.optionList[0].summary = ringtone.title
                    }
                }
                ContentSelectorActivity.VIBRATION_REQUEST_CODE -> {
                    data?.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                        val vibration = it as PatternItem
                        viewModel.vibration.value = vibration
                        viewModel.optionList[1].summary = vibration.title
                    }
                }
                ContentSelectorActivity.SNOOZE_REQUEST_CODE -> {
                    data?.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                        val snooze = it as SnoozeItem
                        viewModel.snooze.value = snooze
                        viewModel.optionList[2].summary = snooze.title
                    }
                }
                ContentSelectorActivity.DATE_REQUEST_CODE -> {
                    data?.getLongExtra(ContentSelectorActivity.START_DATE_KEY, -1)?.let {
                        viewModel.startDate =
                                if(it > 0) {
                                    val startInstant = Instant.ofEpochMilli(it)
                                    ZonedDateTime.ofInstant(startInstant, ZoneId.of(viewModel.timeZone.value))
                                            .withHour(viewModel.remoteZonedDateTime.hour)
                                            .withMinute(viewModel.remoteZonedDateTime.minute)
                                }
                                else null
                    }

                    data?.getLongExtra(ContentSelectorActivity.END_DATE_KEY, -1)?.let {
                        viewModel.endDate =
                                if(it > 0) {
                                    val endInstant = Instant.ofEpochMilli(it)
                                    ZonedDateTime.ofInstant(endInstant, ZoneId.of(viewModel.timeZone.value))
                                            .withHour(viewModel.remoteZonedDateTime.hour)
                                            .withMinute(viewModel.remoteZonedDateTime.minute)
                                }
                                else null
                    }

                    viewModel.startDate.let {
                        viewModel.remoteZonedDateTime =
                        if(it == null) {
                            viewModel.remoteZonedDateTime
                                    .withYear(now.year)
                                    .withMonth(now.monthValue)
                                    .withDayOfMonth(now.dayOfMonth)
                        }
                        else {
                            viewModel.remoteZonedDateTime
                                    .withYear(it.year)
                                    .withMonth(it.monthValue)
                                    .withDayOfMonth(it.dayOfMonth)
                        }
                    }

                    date.text = formatDate()
                    updateTimeZone()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.menu_alarm_generator, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.show_dst -> {
                true
            }
            else -> false
        }
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.time_zone_view -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && timeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
                    val i = Intent(fragmentContext, TimeZonePickerActivity::class.java).apply {
                        putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_CHANGE)
                        putExtra(TimeZonePickerActivity.TIME_ZONE_ID, viewModel.timeZone.value)
                        putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_ALARM_CLOCK)
                    }
                    startActivityForResult(i, TimeZoneSearchActivity.TIME_ZONE_REQUEST_CODE)
                }
                else startActivityForResult(Intent(fragmentContext, TimeZoneSearchActivity::class.java), TimeZoneSearchActivity.TIME_ZONE_REQUEST_CODE)
            }
            R.id.date_view -> {
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_DATE
                    putExtra(ContentSelectorActivity.START_DATE_KEY,viewModel.startDate?.toInstant()?.toEpochMilli())
                    putExtra(ContentSelectorActivity.END_DATE_KEY, viewModel.endDate?.toInstant()?.toEpochMilli())
                    putExtra(ContentSelectorActivity.TIME_ZONE_KEY, viewModel.timeZone.value)
                }
                startActivityForResult(contentIntent, ContentSelectorActivity.DATE_REQUEST_CODE)
            }
            R.id.action -> {
                saveAlarm()
            }
        }
    }

    override fun onItemClick(view: View, position: Int, item: OptionItem) {
        when(position) {
            0 -> { // Ringtone
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_AUDIO
                    putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, viewModel.ringtone.value)
                }
                startActivityForResult(contentIntent, ContentSelectorActivity.AUDIO_REQUEST_CODE)
            }
            1 -> { // Vibration
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_VIBRATION
                    putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, viewModel.vibration.value)
                }
                startActivityForResult(contentIntent, ContentSelectorActivity.VIBRATION_REQUEST_CODE)
            }
            2 -> { // Snooze
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_SNOOZE
                    putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, viewModel.snooze.value)
                }
                startActivityForResult(contentIntent, ContentSelectorActivity.SNOOZE_REQUEST_CODE)
            }
            3 -> { // Label
                if(!labelDialog.isAdded) labelDialog.show(parentFragmentManager, AlarmGeneratorActivity.TAG_FRAGMENT_LABEL)
            }
            4 -> { // Color Tag
                if(!colorTagDialog.isAdded) colorTagDialog.show(parentFragmentManager, AlarmGeneratorActivity.TAG_FRAGMENT_COLOR_TAG)
            }
        }
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
        if(viewModel.recurrences.value == null) viewModel.recurrences.value = IntArray(7) { 0 }
        when(checkedId) {
            R.id.sunday -> {
                viewModel.recurrences.value?.set(0, if(isChecked) 1 else 0)
            }
            R.id.monday -> {
                viewModel.recurrences.value?.set(1, if(isChecked) 2 else 0)
            }
            R.id.tuesday -> {
                viewModel.recurrences.value?.set(2, if(isChecked) 3 else 0)
            }
            R.id.wednesday -> {
                viewModel.recurrences.value?.set(3, if(isChecked) 4 else 0)
            }
            R.id.thursday -> {
                viewModel.recurrences.value?.set(4, if(isChecked) 5 else 0)
            }
            R.id.friday -> {
                viewModel.recurrences.value?.set(5, if(isChecked) 6 else 0)
            }
            R.id.saturday -> {
                viewModel.recurrences.value?.set(6, if(isChecked) 7 else 0)
            }
        }

        updateTimeZone()
    }

    override fun onTimeChanged(picker: TimePicker?, hour: Int, minute: Int) {
        viewModel.remoteZonedDateTime =
                viewModel.remoteZonedDateTime
                        .withHour(hour)
                        .withMinute(minute)

        viewModel.startDate =
                viewModel.startDate
                        ?.withHour(hour)
                        ?.withMinute(minute)
        viewModel.endDate =
                viewModel.endDate
                        ?.withHour(hour)
                        ?.withMinute(minute)

        updateTimeZone()
    }

    private fun updateTimeZone() {
        val timeSet = viewModel.remoteZonedDateTime.toInstant().toEpochMilli()
        val difference = TimeZone.getTimeZone(viewModel.timeZone.value).getOffset(timeSet) - TimeZone.getDefault().getOffset(timeSet)
        val offset =
                if(difference == 0) resources.getString(R.string.current_time_zone)
                else MediaCursor.getOffsetOfDifference(fragmentContext, difference, MediaCursor.TYPE_CURRENT)

        var resultInLocal = viewModel.alarmController.calculateDateTime(createAlarm(), TYPE_ALARM)
        if(resultInLocal != null) {
            resultInLocal = resultInLocal.withZoneSameInstant(ZoneId.systemDefault())
            expected_time.text = resultInLocal.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))
        }

        time_zone_offset.text = offset

        val setTimeZone = TimeZone.getTimeZone(viewModel.timeZone.value)
        time_zone_dst.visibility =
                if(setTimeZone.useDaylightTime() && setTimeZone.inDaylightTime(Date(viewModel.remoteZonedDateTime.toInstant().toEpochMilli()))) View.VISIBLE
                else View.GONE
    }

    private fun getAlarmOptions(): ArrayList<OptionItem> {
        val array = ArrayList<OptionItem>()
        val options = resources.getStringArray(R.array.alarm_options)

        val values = arrayOf(
                viewModel.ringtone.value?.title,
                viewModel.vibration.value?.title,
                viewModel.snooze.value?.title,
                viewModel.label.value,
                viewModel.colorTag.value?.toString())

        options.forEachIndexed { index, s ->
            array.add(OptionItem(s, values[index]))
        }

        return array
    }

    private fun getFormattedTimeZoneName(timeZoneId: String?): String {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(timeZoneId))
        }
        else timeZoneId ?: getString(R.string.time_zone_unknown)
    }

    private fun formatDate(): String {
        val s = viewModel.startDate?.withZoneSameLocal(ZoneId.systemDefault())?.toInstant()
        val e = viewModel.endDate?.withZoneSameLocal(ZoneId.systemDefault())?.toInstant()

        return when {
            s != null && e != null -> {
                DateUtils.formatDateRange(fragmentContext, s.toEpochMilli(), e.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
            }
            s != null -> {
                viewModel.recurrences.value.let { array ->
                    if(array != null && array.any { it > 0 }) {
                        fragmentContext.getString(R.string.range_begin).format(DateUtils.formatDateTime(context, s.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
                    }
                    else null
                } ?: DateUtils.formatDateTime(fragmentContext, s.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL)
            }
            e != null -> {
                fragmentContext.getString(R.string.range_until).format(DateUtils.formatDateTime(fragmentContext, e.toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL))
            }
            else -> {
                getString(R.string.range_not_set)
            }
        }
    }

    private fun getLabelDialog(): LabelDialogFragment {
        var dialog = parentFragmentManager.findFragmentByTag(AlarmGeneratorActivity.TAG_FRAGMENT_LABEL) as? LabelDialogFragment
        if(dialog == null) dialog = LabelDialogFragment.newInstance()

        viewModel.label.value?.let {
            dialog.setLastLabel(it)
        }
        dialog.setOnDialogEventListener(object: LabelDialogFragment.OnDialogEventListener {
            override fun onPositiveButtonClick(inter: DialogInterface, label: String) {
                viewModel.label.value = label
                viewModel.optionList[3].summary = label
                dialog.setLastLabel(label)
            }

            override fun onNegativeButtonClick(inter: DialogInterface) { inter.cancel() }

            override fun onNeutralButtonClick(inter: DialogInterface) {
                viewModel.label.value = null
                dialog.setLastLabel("")
            }
        })
        return dialog
    }

    private fun getColorTagChoiceDialog(): ColorTagDialogFragment {
        var dialog = parentFragmentManager.findFragmentByTag(AlarmGeneratorActivity.TAG_FRAGMENT_COLOR_TAG) as? ColorTagDialogFragment
        if(dialog == null) dialog = ColorTagDialogFragment.newInstance()
        dialog.setLastChoice(viewModel.colorTag.value ?: 0)
        dialog.setOnDialogEventListener(object: ColorTagDialogFragment.OnDialogEventListener {
            override fun onPositiveButtonClick(inter: DialogInterface, color: Int) {
                viewModel.colorTag.value = color
                viewModel.optionList[4].summary = color.toString()
                dialog.setLastChoice(color)
            }

            override fun onNegativeButtonClick(inter: DialogInterface, index: Int) { inter.cancel() }

            override fun onNeutralButtonClick(inter: DialogInterface, index: Int) {
                viewModel.colorTag.value = 0
                dialog.setLastChoice(0)
            }

        })
        return dialog
    }

    private fun createAlarm(): AlarmItem {
        val notificationId = 100000 + Random().nextInt(899999)

        return AlarmItem(
                id = viewModel.alarmItem?.id,
                timeZone = viewModel.timeZone.value ?: TimeZone.getDefault().id,
                timeSet = viewModel.remoteZonedDateTime.toInstant().toEpochMilli().toString(),
                repeat = viewModel.recurrences.value ?: intArrayOf(0, 0, 0, 0, 0, 0, 0),
                ringtone = viewModel.ringtone.value?.uri,
                vibration = viewModel.vibration.value?.array,
                snooze = viewModel.snooze.value?.duration ?: 0,
                label = viewModel.label.value,
                on_off = 1,
                notiId = viewModel.alarmItem?.notiId ?: notificationId,
                colorTag = viewModel.colorTag.value ?: 0,
                index = viewModel.alarmItem?.index ?: -1,
                startDate = viewModel.startDate?.toInstant()?.toEpochMilli(),
                endDate = viewModel.endDate?.toInstant()?.toEpochMilli(),
                pickerTime = viewModel.remoteZonedDateTime.toInstant().toEpochMilli()
        )
    }

    private fun saveAlarm() {
        val item = createAlarm()
        val start = viewModel.startDate?.toInstant()
        val end = viewModel.endDate?.toInstant()

        when {
            start != null && end != null -> {
                if(start.toEpochMilli() >= end.toEpochMilli() || end.toEpochMilli() <= System.currentTimeMillis()) {
                    Snackbar.make(fragment_container, getString(R.string.end_date_earlier_than_start_date), Snackbar.LENGTH_SHORT)
                            .setAnchorView(action)
                            .show()
                    return
                }

                // alarm will be fired everyday if date range is less than a week.
                val difference = end.toEpochMilli() - start.toEpochMilli()
                if(TimeUnit.MILLISECONDS.toDays(difference) > 6) {
                    viewModel.recurrences.value.let { recurrences ->
                        if(recurrences == null || recurrences.all { it == 0 }) {
                            Snackbar.make(fragment_container, getString(R.string.must_check_repeat), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(action)
                                    .show()
                            return
                        }
                    }
                }
            }
            start != null -> {
                viewModel.recurrences.value?.let { recurrences ->
                    if(!recurrences.any { it > 0 } && start.toEpochMilli() < System.currentTimeMillis()) {
                        Snackbar.make(fragment_container, getString(R.string.start_date_and_time_is_wrong), Snackbar.LENGTH_SHORT)
                                .setAnchorView(action)
                                .show()
                        return
                    }
                }
            }
            end != null -> {
                if(end.toEpochMilli() < System.currentTimeMillis()) {
                    Snackbar.make(fragment_container, getString(R.string.end_date_earlier_than_today), Snackbar.LENGTH_SHORT)
                            .setAnchorView(action)
                            .show()
                    return
                }

                val difference = end.toEpochMilli() - System.currentTimeMillis()
                when(TimeUnit.MILLISECONDS.toDays(difference)) {
                    in 1..6 -> {
                        val expect = try {
                            viewModel.alarmController.calculateDateTime(item, TYPE_ALARM)
                        } catch (e: IllegalStateException) {
                            null
                        }

                        val now = ZonedDateTime.now()
                        if(expect == null || expect.isBefore(now) || expect.isEqual(now) || expect.isAfter(viewModel.endDate)) {
                            Snackbar.make(fragment_container, getString(R.string.invalid_repeat), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(action)
                                    .show()
                            return
                        }
                    }
                    else -> {
                        viewModel.recurrences.value.let { recurrences ->
                            if(recurrences == null || recurrences.all { it == 0 }) {
                                Snackbar.make(fragment_container, getString(R.string.must_check_repeat), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(action)
                                        .show()
                                return
                            }
                        }
                    }
                }
            }
        }

        val scheduledTime = viewModel.alarmController.scheduleLocalAlarm(fragmentContext, item, TYPE_ALARM)
        if(scheduledTime == -1L) {
            Snackbar.make(fragment_container, getString(R.string.unable_to_create_alarm), Snackbar.LENGTH_SHORT)
                    .setAnchorView(action)
                    .show()
            return
        }

        item.timeSet = scheduledTime.toString()

        val dbCursor = DatabaseCursor(fragmentContext)
        if(viewModel.alarmItem == null) {
            item.id = dbCursor.insertAlarm(item).toInt()
            item.index = item.id
        }
        else {
            dbCursor.updateAlarm(item)
        }

        val zoneId = viewModel.remoteZonedDateTime.zone
        val timeZone = TimeZone.getTimeZone(zoneId.id)

        if(timeZone.useDaylightTime()) {
            val nextDateTimeAfter = zoneId.rules.nextTransition(Instant.now()).dateTimeAfter.atZone(zoneId).toInstant().toEpochMilli()
            val dstId = dbCursor.insertDst(nextDateTimeAfter, zoneId.id, item.id)
            DstController(fragmentContext).checkAndScheduleDst(DstItem(dstId, nextDateTimeAfter, zoneId.id, item.id))
        }

        activity?.run {
            if(isTaskRoot) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = scheduledTime
                }
                Snackbar.make(fragment_container, getString(R.string.alarm_on, MediaCursor.getRemainTime(fragmentContext, calendar)), Snackbar.LENGTH_SHORT)
                        .setAnchorView(action)
                        .show()
            }
        }

        activity?.run {
            val intent = Intent()
            val bundle = Bundle().apply {
                putParcelable(AlarmReceiver.ITEM, item)
                putLong(AlarmActivity.SCHEDULED_TIME, scheduledTime)
            }
            intent.putExtra(AlarmReceiver.OPTIONS, bundle)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private inner class DateTimeChangedReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                MainActivity.ACTION_UPDATE_ALL -> {
                    if(viewModel.startDate == null) {
                        val dateTime = ZonedDateTime.now(ZoneId.of(viewModel.timeZone.value))
                        viewModel.remoteZonedDateTime =
                                viewModel.remoteZonedDateTime
                                        .withYear(dateTime.year)
                                        .withMonth(dateTime.monthValue)
                                        .withDayOfMonth(dateTime.dayOfMonth)
                        updateTimeZone()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "AlarmGeneratorFragment"

        @JvmStatic
        fun newInstance() = AlarmGeneratorFragment()
    }
}
