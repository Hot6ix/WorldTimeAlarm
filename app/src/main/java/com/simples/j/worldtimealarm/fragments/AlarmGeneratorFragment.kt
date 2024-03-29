package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.databinding.FragmentAlarmGeneratorBinding
import com.simples.j.worldtimealarm.etc.*
import com.simples.j.worldtimealarm.models.AlarmGeneratorViewModel
import com.simples.j.worldtimealarm.support.AlarmOptionAdapter
import com.simples.j.worldtimealarm.utils.AlarmController.TYPE_ALARM
import com.simples.j.worldtimealarm.utils.AlarmStringFormatHelper
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.coroutines.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.TextStyle
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

class AlarmGeneratorFragment : Fragment(), CoroutineScope, AlarmOptionAdapter.OnItemClickListener, View.OnClickListener, MaterialButtonToggleGroup.OnButtonCheckedListener, TimePicker.OnTimeChangedListener {

    // init in main thread
    private lateinit var fragmentContext: Context
    private lateinit var binding: FragmentAlarmGeneratorBinding
    private lateinit var viewModel: AlarmGeneratorViewModel
    private lateinit var labelDialog: LabelDialogFragment
    private lateinit var colorTagDialog: ColorTagDialogFragment
    private lateinit var db: AppDatabase
    private lateinit var preference: SharedPreferences

    // init in coroutine launch
    private lateinit var alarmOptionAdapter: AlarmOptionAdapter
    private lateinit var ringtoneList: ArrayList<RingtoneItem>
    private lateinit var vibratorPatternList: ArrayList<PatternItem>
    private lateinit var snoozeList: ArrayList<SnoozeItem>

    private var now = ZonedDateTime.now()
    private var is24HourMode = false

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private var initJob: Job = Job()
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineExceptionHandler

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()

        crashlytics.recordException(throwable)
        if(activity?.isFinishing == false) {
            Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            activity?.finish()
        }
    }

    private var timeZoneSelectorOption: String = ""
    private val dateTimeChangedReceiver = DateTimeChangedReceiver()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentAlarmGeneratorBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        db = Room.databaseBuilder(fragmentContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
            .build()

        preference = PreferenceManager.getDefaultSharedPreferences(fragmentContext)
        is24HourMode = preference.getBoolean(fragmentContext.getString(R.string.setting_24_hr_clock_key), false)
        timeZoneSelectorOption = preference.getString(resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD)
            ?: SettingFragment.SELECTOR_OLD

        activity?.run {
            viewModel = ViewModelProvider(this)[AlarmGeneratorViewModel::class.java]
        }

        initJob = launch(coroutineExceptionHandler) {
            val userRingtone = db.ringtoneItemDao().getAll()
            val systemRingtone = MediaCursor.getRingtoneList(fragmentContext)
            // use first ringtone if phone has system ringtone, if not set to no ringtone.
            val defaultRingtone = if(systemRingtone.size > 1) systemRingtone[1] else systemRingtone[0]

            ringtoneList = ArrayList<RingtoneItem>().apply {
                addAll(userRingtone)
                addAll(systemRingtone)
            }

            vibratorPatternList = MediaCursor.getVibratorPatterns(fragmentContext)
            snoozeList = MediaCursor.getSnoozeList(fragmentContext)

            // init values
            viewModel.alarmItem.let {
                if (it != null) {
                    viewModel.timeZone.value = it.timeZone.replace(" ", "_")
                    now = ZonedDateTime.now(ZoneId.of(viewModel.timeZone.value))

                    val instant = Instant.ofEpochMilli(it.pickerTime)
                    val local = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
                    viewModel.remoteZonedDateTime = local.withZoneSameInstant(ZoneId.of(it.timeZone))

                    it.startDate?.let { startDateInMillis ->
                        if (startDateInMillis > 0) {
                            val startInstant = Instant.ofEpochMilli(startDateInMillis)
                            viewModel.startDate = ZonedDateTime.ofInstant(startInstant, ZoneId.of(viewModel.timeZone.value))
                                .withHour(viewModel.remoteZonedDateTime.hour)
                                .withMinute(viewModel.remoteZonedDateTime.minute)
                        }
                    }

                    it.endDate?.let { endDateInMillis ->
                        if (endDateInMillis > 0) {
                            val endInstant = Instant.ofEpochMilli(endDateInMillis)
                            viewModel.endDate = ZonedDateTime.ofInstant(endInstant, ZoneId.of(viewModel.timeZone.value))
                                .withHour(viewModel.remoteZonedDateTime.hour)
                                .withMinute(viewModel.remoteZonedDateTime.minute)
                        }
                    }

                    viewModel.startDate.let { start ->
                        if (start == null) {
                            viewModel.remoteZonedDateTime =
                                viewModel.remoteZonedDateTime
                                    .withYear(now.year)
                                    .withMonth(now.monthValue)
                                    .withDayOfMonth(now.dayOfMonth)
                        } else {
                            viewModel.remoteZonedDateTime =
                                viewModel.remoteZonedDateTime
                                    .withYear(start.year)
                                    .withMonth(start.monthValue)
                                    .withDayOfMonth(start.dayOfMonth)
                        }
                    }

                    // recurrences
                    viewModel.recurrences.value = it.repeat

                    // options
                    viewModel.ringtone.value = ringtoneList.find { item -> item.uri == it.ringtone } ?: defaultRingtone
                    viewModel.vibration.value = vibratorPatternList.find { item ->
                        if (item.array == null && it.vibration == null) true
                        else item.array?.contentEquals(it.vibration ?: longArrayOf(0)) ?: false
                    }
                    viewModel.snooze.value = snoozeList.find { item -> item.duration == it.snooze }
                    viewModel.label.value = it.label
                    viewModel.colorTag.value = it.colorTag
                }
                else {
                    viewModel.timeZone.value = TimeZone.getDefault().id

                    viewModel.ringtone.value = defaultRingtone
                    viewModel.vibration.value = vibratorPatternList[0]
                    viewModel.snooze.value = snoozeList[0]
                }
            }

            // init recurrence button group
            setupRecurrences()

            // init time
            val hour = viewModel.remoteZonedDateTime.hour
            val minute = viewModel.remoteZonedDateTime.minute
            if (Build.VERSION.SDK_INT < 23) {
                @Suppress("DEPRECATION")
                binding.timePicker.currentHour = hour
                @Suppress("DEPRECATION")
                binding.timePicker.currentMinute = minute
            } else {
                binding.timePicker.hour = hour
                binding.timePicker.minute = minute
            }

            // init alarm options
            viewModel.optionList = getAlarmOptions()
            alarmOptionAdapter = AlarmOptionAdapter(viewModel.optionList, fragmentContext)
            alarmOptionAdapter.setOnItemClickListener(this@AlarmGeneratorFragment)

            binding.alarmOptions.apply {
                adapter = alarmOptionAdapter
                layoutManager = LinearLayoutManager(fragmentContext, LinearLayoutManager.VERTICAL, false)

                addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
                isNestedScrollingEnabled = false
            }

            // init observers
            viewModel.ringtone.observe(viewLifecycleOwner) {
                alarmOptionAdapter.notifyItemChanged(0)
            }
            viewModel.vibration.observe(viewLifecycleOwner) {
                alarmOptionAdapter.notifyItemChanged(1)
            }
            viewModel.snooze.observe(viewLifecycleOwner) {
                alarmOptionAdapter.notifyItemChanged(2)
            }
            viewModel.label.observe(viewLifecycleOwner) {
                alarmOptionAdapter.notifyItemChanged(3)
            }
            viewModel.colorTag.observe(viewLifecycleOwner) {
                alarmOptionAdapter.notifyItemChanged(4)
            }

            // init timezone
            binding.timeZoneName.text = getFormattedTimeZoneName(viewModel.timeZone.value)

            // init date
            binding.date.text = AlarmStringFormatHelper.formatDate(
                fragmentContext,
                viewModel.startDate,
                viewModel.endDate,
                viewModel.recurrences.value?.any { it > 0 } ?: false
            )

            // init action button
            binding.action.apply {
                if (viewModel.alarmItem != null) {
                    text = getString(R.string.apply)
                    icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_done_white)
                } else {
                    text = getString(R.string.create)
                    icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_add)
                }
            }

            // show components & hide progress bar
            binding.detailContentLayout.visibility = View.VISIBLE
            binding.action.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }

        // init dialog
        labelDialog = getLabelDialog()
        colorTagDialog = getColorTagChoiceDialog()

        binding.timePicker.setIs24HourView(is24HourMode)
        binding.timePicker.setOnTimeChangedListener(this)
        binding.timeZoneView.setOnClickListener(this)

        binding.dateView.setOnClickListener(this)
        binding.dayRecurrence.addOnButtonCheckedListener(this)
        binding.action.setOnClickListener(this)
        binding.detailContentLayout.isNestedScrollingEnabled = false

        val intentFilter = IntentFilter().apply {
            addAction(MainActivity.ACTION_UPDATE_ALL)
            addAction(Intent.ACTION_TIME_TICK)
        }
        fragmentContext.registerReceiver(dateTimeChangedReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()

        updateEstimated()
    }

    override fun onDestroy() {
        super.onDestroy()

        launch(coroutineContext) {
            initJob.cancelAndJoin()
            job.cancelAndJoin()

            try {
                fragmentContext.unregisterReceiver(dateTimeChangedReceiver)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    private val onTimeZoneActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "onTimeZoneActivityResult()")
        launch(coroutineContext) {
            initJob.join()

            result.data?.let { data ->
                if(data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    val replacedTz = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)?.replace(" ", "_")
                    replacedTz?.let {
                        viewModel.timeZone.value = it.replace(" ", "_")

                        viewModel.remoteZonedDateTime =
                            viewModel.remoteZonedDateTime
                                .withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))

                        viewModel.startDate = viewModel.startDate?.withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))
                        viewModel.endDate = viewModel.endDate?.withZoneSameLocal(ZoneId.of(viewModel.timeZone.value))
                    }

                    binding.timeZoneName.text = getFormattedTimeZoneName(replacedTz)
                    updateEstimated()
                    setupRecurrences()
                }
            }
        }
    }

    private val onRingtoneSelectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "onRingtoneSelectionActivityResult()")
        launch(coroutineContext) {
            initJob.join()

            result.data?.let { data ->
                data.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                    val ringtone = it as RingtoneItem
                    viewModel.ringtone.value = ringtone
                    viewModel.optionList[0].summary = ringtone.title
                }
            }
        }
    }

    private val onVibrationSelectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "onVibrationSelectionActivityResult()")
        launch(coroutineContext) {
            initJob.join()

            result.data?.let { data ->
                data.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                    val vibration = it as PatternItem
                    viewModel.vibration.value = vibration
                    viewModel.optionList[1].summary = vibration.title
                }
            }
        }
    }

    private val onSnoozeSelectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "onSnoozeSelectionActivityResult()")
        launch(coroutineContext) {
            initJob.join()

            result.data?.let { data ->
                data.getSerializableExtra(ContentSelectorActivity.LAST_SELECTED_KEY)?.also {
                    val snooze = it as SnoozeItem
                    viewModel.snooze.value = snooze
                    viewModel.optionList[2].summary = snooze.title
                }
            }
        }
    }

    private val onDateSelectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "onDateSelectionActivityResult()")
        launch(coroutineContext) {
            initJob.join()

            result.data?.let { data ->
                data.getLongExtra(ContentSelectorActivity.START_DATE_KEY, -1).let {
                    viewModel.startDate =
                        if(it > 0) {
                            val startInstant = Instant.ofEpochMilli(it)
                            ZonedDateTime.ofInstant(startInstant, ZoneId.of(viewModel.timeZone.value))
                                .withHour(viewModel.remoteZonedDateTime.hour)
                                .withMinute(viewModel.remoteZonedDateTime.minute)
                        }
                        else null
                }

                data.getLongExtra(ContentSelectorActivity.END_DATE_KEY, -1).let {
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

                binding.date.text = AlarmStringFormatHelper.formatDate(
                    fragmentContext,
                    viewModel.startDate,
                    viewModel.endDate,
                    viewModel.recurrences.value?.any { it > 0 } ?: false
                )
                updateEstimated()
            }
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
                    onTimeZoneActivityResult.launch(i)
                }
                else onTimeZoneActivityResult.launch(Intent(fragmentContext, TimeZoneSearchActivity::class.java))
            }
            R.id.date_view -> {
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_DATE
                    putExtra(ContentSelectorActivity.START_DATE_KEY,viewModel.startDate?.toInstant()?.toEpochMilli())
                    putExtra(ContentSelectorActivity.END_DATE_KEY, viewModel.endDate?.toInstant()?.toEpochMilli())
                    putExtra(ContentSelectorActivity.TIME_ZONE_KEY, viewModel.timeZone.value)
                }
                onDateSelectionActivityResult.launch(contentIntent)
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
                onRingtoneSelectionActivityResult.launch(contentIntent)
            }
            1 -> { // Vibration
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_VIBRATION
                    putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, viewModel.vibration.value)
                }
                onVibrationSelectionActivityResult.launch(contentIntent)
            }
            2 -> { // Snooze
                val contentIntent = Intent(fragmentContext, ContentSelectorActivity::class.java).apply {
                    action = ContentSelectorActivity.ACTION_REQUEST_SNOOZE
                    putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, viewModel.snooze.value)
                }
                onSnoozeSelectionActivityResult.launch(contentIntent)
            }
            3 -> { // Label
                viewModel.label.value?.let {
                    labelDialog.setLastLabel(it)
                }

                if(!labelDialog.isAdded) labelDialog.show(parentFragmentManager, AlarmGeneratorActivity.TAG_FRAGMENT_LABEL)
            }
            4 -> { // Color Tag
                if(!colorTagDialog.isAdded) colorTagDialog.show(parentFragmentManager, AlarmGeneratorActivity.TAG_FRAGMENT_COLOR_TAG)
            }
        }
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
        if(viewModel.recurrences.value == null) viewModel.recurrences.value = IntArray(7) { 0 }

        launch {
            initJob.join()

            when(checkedId) {
                R.id.firstDayOfWeek -> {
                    viewModel.recurrences.value?.set(0, if(isChecked) (binding.firstDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.secondDayOfWeek -> {
                    viewModel.recurrences.value?.set(1, if(isChecked) (binding.secondDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.thirdDayOfWeek -> {
                    viewModel.recurrences.value?.set(2, if(isChecked) (binding.thirdDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.fourthDayOfWeek -> {
                    viewModel.recurrences.value?.set(3, if(isChecked) (binding.fourthDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.fifthDayOfWeek -> {
                    viewModel.recurrences.value?.set(4, if(isChecked) (binding.fifthDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.sixthDayOfWeek -> {
                    viewModel.recurrences.value?.set(5, if(isChecked) (binding.sixthDayOfWeek.tag as DayOfWeek).value else 0)
                }
                R.id.seventhDayOfWeek -> {
                    viewModel.recurrences.value?.set(6, if(isChecked) (binding.seventhDayOfWeek.tag as DayOfWeek).value else 0)
                }
            }

            withContext(Dispatchers.Main) {
                binding.date.text = AlarmStringFormatHelper.formatDate(
                    fragmentContext,
                    viewModel.startDate,
                    viewModel.endDate,
                    viewModel.recurrences.value?.any { it > 0 } ?: false
                )
                updateEstimated()
            }
        }
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

        updateEstimated()
    }

    private fun updateEstimated() {
        job = launch(coroutineExceptionHandler) {
            val result = withContext(Dispatchers.IO) {
                viewModel.alarmController.calculateDateTime(createAlarm(), TYPE_ALARM)
            }

            if(result != null) {
                val resultInLocal = result.withZoneSameInstant(ZoneId.systemDefault())
                viewModel.estimated = resultInLocal

                if(createAlarm().isExpired(resultInLocal)) {
                    binding.estTimeZone.setTextColor(ContextCompat.getColor(fragmentContext, R.color.color1))
                    binding.estDateTime.setTextColor(ContextCompat.getColor(fragmentContext, R.color.color1))
                    binding.estIcon.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.ic_warning))
                    binding.estIcon.setColorFilter(ContextCompat.getColor(fragmentContext, R.color.color1))
                }
                else {
                    binding.estTimeZone.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                    binding.estDateTime.setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColor))
                    binding.estIcon.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.ic_event_available))
                    binding.estIcon.setColorFilter(ContextCompat.getColor(fragmentContext, R.color.color10))
                }

                val diff = ZoneId.systemDefault().rules.getOffset(resultInLocal.toInstant()).totalSeconds - viewModel.remoteZonedDateTime.zone.rules.getOffset(resultInLocal.toInstant()).totalSeconds

                val diffHour = diff.div(60 * 60)
                val diffMin = diff.rem(60 * 60).div(60)

                val hourFormat =
                        if(diffHour < 0 || diffMin < 0)
                            DecimalFormat("-00")
                        else
                            DecimalFormat("+00")


                val minFormat = DecimalFormat("00")
                val diffText = "${hourFormat.format(diffHour.absoluteValue)}:${minFormat.format(diffMin.absoluteValue)}"

                binding.estTimeZone.text = getString(R.string.time_zone_with_diff, getFormattedTimeZoneName(resultInLocal.zone.id), diffText)
                binding.estDateTime.text = DateFormat.format(MediaCursor.getLocalizedDateTimeFormat(is24HourMode), resultInLocal.toInstant().toEpochMilli())
//                binding.estDateTime.text = DateUtils.formatDateTime(fragmentContext, resultInLocal.toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL)
            }
        }
    }

    private fun setupRecurrences() {
        val locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                MediaCursor.getULocaleByTimeZoneId(viewModel.timeZone.value)?.toLocale() ?: Locale.getDefault()
            else Locale.getDefault()

        MediaCursor.getWeekDaysInLocale(locale).forEachIndexed { index, dayOfWeek ->
            (binding.dayRecurrence.getChildAt(index) as MaterialButton).apply {
                text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                tag = dayOfWeek

                // set color for weekend
                // color will be red if weekendCease and weekendOnset are same
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    with(android.icu.util.Calendar.getInstance().weekData) {
                        when(dayOfWeek.value) {
                            // weekend finish
                            MediaCursor.getDayOfWeekValueFromCalendarToThreeTenBp(weekendCease) -> {
                                (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_red_light))
                            }
                            // weekend start
                            MediaCursor.getDayOfWeekValueFromCalendarToThreeTenBp(weekendOnset) -> {
                                (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, android.R.color.holo_blue_light))
                            }
                            else -> {
                                (this@apply).setTextColor(ContextCompat.getColor(fragmentContext, R.color.textColorEnabled))
                            }
                        }
                    }
                }
            }
        }

        // restore checked recurrences
        viewModel.recurrences.value?.let { array ->
            val checked = array.filter { recurrence ->
                recurrence > 0
            }

            binding.dayRecurrence.children.forEach { view ->
                with(view as MaterialButton) {
                    val dayOfWeek = tag as DayOfWeek

                    if(checked.contains(dayOfWeek.value)) {
                        binding.dayRecurrence.check(this.id)
                    }
                    else {
                        binding.dayRecurrence.uncheck(this.id)
                    }
                }

            }
        }
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
        timeZoneId?.let {
            return try {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(it))
                else it
            } catch (e: Exception) {
                e.printStackTrace()

                crashlytics.recordException(e.fillInStackTrace())
                it
            }
        }

        return getString(R.string.time_zone_unknown)
    }

    private fun getLabelDialog(): LabelDialogFragment {
        var dialog = parentFragmentManager.findFragmentByTag(AlarmGeneratorActivity.TAG_FRAGMENT_LABEL) as? LabelDialogFragment
        if(dialog == null) dialog = LabelDialogFragment.newInstance()

        dialog.setOnDialogEventListener(object: LabelDialogFragment.OnDialogEventListener {
            override fun onPositiveButtonClick(inter: DialogInterface, label: String) {
                viewModel.label.value = label
                viewModel.optionList[3].summary = label
                dialog.setLastLabel(label)
            }

            override fun onNegativeButtonClick(inter: DialogInterface) { inter.cancel() }

            override fun onNeutralButtonClick(inter: DialogInterface) {}
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
            viewModel.timeZone.value.isNullOrEmpty() -> {
                Snackbar.make(binding.fragmentContainer, getString(R.string.time_zone_select), Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.action)
                        .show()
                return
            }
            start != null && end != null -> {
                if(start.toEpochMilli() >= end.toEpochMilli() || end.toEpochMilli() <= System.currentTimeMillis()) {
                    Snackbar.make(binding.fragmentContainer, getString(R.string.unreachable_alarm), Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.action)
                            .show()
                    return
                }

                // alarm will be fired everyday if date range is less than a week.
                val difference = end.toEpochMilli() - start.toEpochMilli()
                if(TimeUnit.MILLISECONDS.toDays(difference) > 6) {
                    viewModel.recurrences.value.let { recurrences ->
                        if(recurrences == null || recurrences.all { it == 0 }) {
                            Snackbar.make(binding.fragmentContainer, getString(R.string.must_check_repeat), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(binding.action)
                                    .show()
                            return
                        }
                    }
                }
                else {
                    if(item.isExpired()) {
                        Snackbar.make(binding.fragmentContainer, getString(R.string.unreachable_alarm), Snackbar.LENGTH_SHORT)
                                .setAnchorView(binding.action)
                                .show()
                        return
                    }
                }
            }
            start != null -> {
                viewModel.recurrences.value?.let { recurrences ->
                    if(!recurrences.any { it > 0 } && start.toEpochMilli() < System.currentTimeMillis()) {
                        Snackbar.make(binding.fragmentContainer, getString(R.string.unreachable_alarm), Snackbar.LENGTH_SHORT)
                                .setAnchorView(binding.action)
                                .show()
                        return
                    }
                }
            }
            end != null -> {
                if(item.isExpired()) {
                    Snackbar.make(binding.fragmentContainer, getString(R.string.unreachable_alarm), Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.action)
                            .show()
                    return
                }

                val difference = end.toEpochMilli() - System.currentTimeMillis()
                when(TimeUnit.MILLISECONDS.toDays(difference)) {
                    !in 0..6 -> {
                        viewModel.recurrences.value.let { recurrences ->
                            if(recurrences == null || recurrences.all { it == 0 }) {
                                Snackbar.make(binding.fragmentContainer, getString(R.string.must_check_repeat), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(binding.action)
                                        .show()
                                return
                            }
                        }
                    }
                }
            }
        }

        // schedule alarm
        val scheduledTime = viewModel.alarmController.scheduleLocalAlarm(fragmentContext, item, TYPE_ALARM)
        if(scheduledTime == -1L) {
            Snackbar.make(binding.fragmentContainer, getString(R.string.unable_to_create_alarm), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.action)
                    .show()
            return
        }

        launch(coroutineExceptionHandler) {
            item.timeSet = scheduledTime.toString()

            if(viewModel.alarmItem == null) {
                item.id = db.alarmItemDao().insert(item).toInt()
                item.index = item.id
            }

            db.alarmItemDao().update(item)

            activity?.run {
                if(isTaskRoot) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = scheduledTime
                    }
                    Snackbar.make(binding.fragmentContainer, getString(R.string.alarm_on, MediaCursor.getRemainTime(fragmentContext, calendar)), Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.action)
                            .show()
                }

                val intent = Intent()
                val bundle = Bundle().apply {
                    putParcelable(AlarmReceiver.ITEM, item)
                    putLong(AlarmGeneratorActivity.SCHEDULED_TIME, scheduledTime)
                }
                intent.putExtra(AlarmReceiver.OPTIONS, bundle)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
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

                        updateEstimated()
                    }
                }
                Intent.ACTION_TIME_TICK -> {
                    val diff = viewModel.remoteZonedDateTime.toEpochSecond() - ZonedDateTime.now().toEpochSecond()
                    if(diff in 0..120) {
                        updateEstimated()
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
