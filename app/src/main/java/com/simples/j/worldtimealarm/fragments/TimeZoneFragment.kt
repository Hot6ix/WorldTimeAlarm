package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.*
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.databinding.FragmentTimeZoneBinding
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.ExtensionHelper
import com.simples.j.worldtimealarm.utils.ExtensionHelper.retryIO
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*
import kotlin.coroutines.CoroutineContext

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneFragment : Fragment(), CoroutineScope, View.OnClickListener {

    private lateinit var fragmentContext: Context
    private lateinit var binding: FragmentTimeZoneBinding
    private lateinit var db: AppDatabase
    private lateinit var preference: SharedPreferences

    private var mPreviousTimeZone: TimeZone? = null
    private var mTimeZone: TimeZone? = null
    private var mTimeZoneInfo: TimeZoneInfo? = null
    private val mDate = Date()
    private var mAction: Int = -1
    private var mType: Int = -1
    private var isTimeZoneExist = false
    private var is24HourMode = false
    private val dateTimeChangedReceiver = DateTimeChangedReceiver()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineExceptionHandler

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()

        crashlytics.recordException(throwable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentTimeZoneBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = Room.databaseBuilder(fragmentContext, AppDatabase::class.java, DatabaseManager.DB_NAME)
            .build()

        (activity as TimeZonePickerActivity).apply {
            supportActionBar?.title = getString(R.string.timezone_fragment_title)
            if(!mTimeZoneId.isNullOrEmpty()) mPreviousTimeZone = TimeZone.getTimeZone(mTimeZoneId)
        }

        preference = PreferenceManager.getDefaultSharedPreferences(fragmentContext)
        is24HourMode = preference.getBoolean(fragmentContext.getString(R.string.setting_24_hr_clock_key), false)

        arguments?.let {
            val id = it.getString(TimeZonePickerActivity.TIME_ZONE_ID)

            mAction = it.getInt(TimeZonePickerActivity.ACTION)
            mType = it.getInt(TimeZonePickerActivity.TYPE)

            if(mAction == TimeZonePickerActivity.ACTION_ADD && id.isNullOrEmpty()) return@let

            if(!id.isNullOrEmpty()) {
                val timeZone = TimeZone.getTimeZone(id)
                mTimeZone = timeZone
                mTimeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), mDate).format(timeZone)
            }
        }

        if(mTimeZone != null && (mPreviousTimeZone != mTimeZone || mAction == TimeZonePickerActivity.ACTION_ADD)) {
            when(mAction) {
                TimeZonePickerActivity.ACTION_ADD -> {
                    launch(coroutineContext) {
                        val clockList = db.clockItemDao().getAll()
                        val item = clockList.find { it.timezone == mTimeZone?.id }

                        isTimeZoneExist = item != null
                    }
                }
                TimeZonePickerActivity.ACTION_CHANGE -> {
//                    val previous = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mPreviousTimeZone), MediaCursor.getGmtOffsetString(locale, mPreviousTimeZone, mDate))
                }
            }
        }

        updateSummariesByTimeZone()

        binding.timeZoneCountryLayout.setOnClickListener(this)
        binding.timeZoneRegionLayout.setOnClickListener(this)
        binding.action.setOnClickListener(this)

        if(mAction == TimeZonePickerActivity.ACTION_ADD) {
            binding.action.apply {
                text = getString(R.string.time_zone_add)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_add)
            }
        }
        else {
            binding.action.apply {
                text = getString(R.string.apply)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_done_white)
            }
        }

        val intentFilter = IntentFilter(MainActivity.ACTION_UPDATE_ALL)
        fragmentContext.registerReceiver(dateTimeChangedReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            fragmentContext.unregisterReceiver(dateTimeChangedReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {
        val bundle = Bundle().apply {
            this.putInt(TimeZonePickerActivity.TYPE, mType)
        }

        when(v.id) {
            R.id.time_zone_country_layout -> {
                (activity as? TimeZonePickerActivity).run {
                    bundle.putInt(TimeZonePickerActivity.REQUEST_TYPE, TimeZonePickerActivity.REQUEST_COUNTRY)
                    this?.startPickerFragment(bundle, TimeZonePickerActivity.TIME_ZONE_PICKER_FRAGMENT_COUNTRY_TAG)
                }
            }
            R.id.time_zone_region_layout -> {
                (activity as? TimeZonePickerActivity).run {
                    bundle.apply {
                        putInt(TimeZonePickerActivity.REQUEST_TYPE, TimeZonePickerActivity.REQUEST_TIME_ZONE)
                        putString(TimeZonePickerActivity.GIVEN_COUNTRY, MediaCursor.getULocaleByTimeZoneId(mTimeZone?.id)?.country)
                    }
                    this?.startPickerFragment(bundle, TimeZonePickerActivity.TIME_ZONE_PICKER_FRAGMENT_TIME_ZONE_TAG)
                }
            }
            R.id.action -> {
                if(mAction == TimeZonePickerActivity.ACTION_ADD) {
                    if(mTimeZone == null) {
                        Snackbar.make(fragment_layout, getString(R.string.time_zone_select), Snackbar.LENGTH_SHORT)
                                .setAnchorView(action)
                                .show()
                        return
                    }
                    else if(isTimeZoneExist) {
                        Snackbar.make(fragment_layout, getString(R.string.exist_timezone), Snackbar.LENGTH_SHORT)
                                .setAnchorView(action)
                                .show()
                        return
                    }
                }

                activity?.run {
                    val intent = Intent()
                    intent.putExtra(TimeZoneSearchActivity.TIME_ZONE_ID, mTimeZone?.id)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    private fun updateSummariesByTimeZone() {
        with(MediaCursor.getCountryNameByTimeZone(mTimeZone)) {
            if(this.isNotEmpty()) binding.timeZoneCountrySummary.text = this
        }

        with(mTimeZoneInfo) {
            if(this == null) {
                binding.timeZoneRegionLayout.isEnabled = false
            }
            else {
                binding.timeZoneRegionLayout.isEnabled = true
                binding.timeZoneRegionSummary.text = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), this.mGmtOffset)

                val timeZoneName = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), MediaCursor.getGmtOffsetString(Locale.getDefault(), mTimeZone, mDate))

                if(mTimeZone.useDaylightTime()) {
                    CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
                        throwable.printStackTrace()
                        crashlytics.recordException(throwable)

                        binding.timeZoneChangeInfo.text = getString(R.string.time_zone_dst_resolve_error_msg, timeZoneName)
                    }).launch {
                        retryIO {
                            val nextTransition = ZoneId.of(mTimeZone.id).rules.nextTransition(Instant.now())
                            val nextTransitionLocal = DateFormat.format(MediaCursor.getLocalizedDateTimeFormat(is24HourMode), ZonedDateTime.of(nextTransition.dateTimeBefore, ZoneId.systemDefault()).toInstant().toEpochMilli())

                            binding.timeZoneChangeInfo.text =
                                if(mTimeZone.inDaylightTime(Date())) {
                                    getString(R.string.time_zone_dst_end_msg, timeZoneName, nextTransitionLocal)
                                }
                                else {
                                    getString(R.string.time_zone_dst_start_msg, timeZoneName, nextTransitionLocal)
                                }
                        }

                    }
                }
                else {
                    binding.timeZoneChangeInfo.text = getString(R.string.time_zone_no_dst_msg, timeZoneName)
                }

                val found = MediaCursor.getULocaleByTimeZoneId(mTimeZone.id)
                val isSingleTimeZone = found != null && (MediaCursor.getTimeZoneListByCountry(found.country).size == 1)
                binding.timeZoneRegionLayout.apply {
                    isEnabled = !isSingleTimeZone
                }
            }
        }
    }

    private inner class DateTimeChangedReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                MainActivity.ACTION_UPDATE_ALL -> {
                    updateSummariesByTimeZone()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimeZoneFragment()
    }

}
