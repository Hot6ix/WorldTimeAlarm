package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.databinding.FragmentTimeZoneBinding
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneFragment : Fragment(), View.OnClickListener {

    private lateinit var fragmentContext: Context
    private lateinit var binding: FragmentTimeZoneBinding

    private var mPreviousTimeZone: TimeZone? = null
    private var mTimeZone: TimeZone? = null
    private var mTimeZoneInfo: TimeZoneInfo? = null
    private val mDate = Date()
    private var mAction: Int = -1
    private var mType: Int = -1
    private var isTimeZoneExist = false
    private val dateTimeChangedReceiver = DateTimeChangedReceiver()

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as TimeZonePickerActivity).apply {
            supportActionBar?.title = getString(R.string.timezone_fragment_title)
            if(!mTimeZoneId.isNullOrEmpty()) mPreviousTimeZone = TimeZone.getTimeZone(mTimeZoneId)
        }

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
                    val clockList = DatabaseCursor(fragmentContext).getClockList()
                    val item = clockList.find { it.timezone == mTimeZone?.id }

                    isTimeZoneExist = item != null
                }
                TimeZonePickerActivity.ACTION_CHANGE -> {
//                    val previous = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mPreviousTimeZone), MediaCursor.getGmtOffsetString(locale, mPreviousTimeZone, mDate))
                }
            }
        }

        updateSummariesByTimeZone()

        binding.timeZoneCountryLayout.setOnClickListener(this)
//        time_zone_country_layout.setOnClickListener(this)
        binding.timeZoneRegionLayout.setOnClickListener(this)
//        time_zone_region_layout.setOnClickListener(this)
        binding.action.setOnClickListener(this)
//        action.setOnClickListener(this)

        if(mAction == TimeZonePickerActivity.ACTION_ADD) {
            binding.action.apply {
                text = getString(R.string.time_zone_add)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_add)
            }
//            action.text = getString(R.string.time_zone_add)
//            action.icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_add)
        }
        else {
            binding.action.apply {
                text = getString(R.string.apply)
                icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_done_white)
            }
//            action.text = getString(R.string.apply)
//            action.icon = ContextCompat.getDrawable(fragmentContext, R.drawable.ic_action_done_white)
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
//            if(this.isNotEmpty()) time_zone_country_summary.text = this
        }

        with(mTimeZoneInfo) {
            if(this == null) {
                binding.timeZoneRegionLayout.isEnabled = false
//                time_zone_region_layout.isEnabled = false
            }
            else {
                binding.timeZoneRegionLayout.isEnabled = true
                binding.timeZoneRegionSummary.text = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), this.mGmtOffset)
//                time_zone_region_layout.isEnabled = true
//                time_zone_region_summary.text = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), this.mGmtOffset)

                val timeZoneName = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), MediaCursor.getGmtOffsetString(Locale.getDefault(), mTimeZone, mDate))

                if(mTimeZone.useDaylightTime()) {
                    val nextTransition = ZoneId.of(mTimeZone.id).rules.nextTransition(Instant.now())
                    val nextTransitionLocal = ZonedDateTime.of(nextTransition.dateTimeBefore, ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))

                    binding.timeZoneChangeInfo.text =
                            if(mTimeZone.inDaylightTime(Date())) {
                                getString(R.string.time_zone_dst_end_msg, timeZoneName, nextTransitionLocal)
                            }
                            else {
                                getString(R.string.time_zone_dst_start_msg, timeZoneName, nextTransitionLocal)
                            }
//                    time_zone_change_info.text =
//                            if(mTimeZone.inDaylightTime(Date())) {
//                                getString(R.string.time_zone_dst_end_msg, timeZoneName, nextTransitionLocal)
//                            }
//                            else {
//                                getString(R.string.time_zone_dst_start_msg, timeZoneName, nextTransitionLocal)
//                            }
                }
                else {
                    binding.timeZoneChangeInfo.text = getString(R.string.time_zone_no_dst_msg, timeZoneName)
//                    time_zone_change_info.text = getString(R.string.time_zone_no_dst_msg, timeZoneName)
                }

                val found = MediaCursor.getULocaleByTimeZoneId(mTimeZone.id)
                val isSingleTimeZone = found != null && (MediaCursor.getTimeZoneListByCountry(found.country).size == 1)
                binding.timeZoneRegionLayout.apply {
                    isEnabled = !isSingleTimeZone
                }
//                time_zone_region_layout.apply {
//                    isEnabled = !isSingleTimeZone
//                }
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
