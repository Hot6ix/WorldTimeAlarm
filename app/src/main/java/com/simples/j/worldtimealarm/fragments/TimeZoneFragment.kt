package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.Intent
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneFragment : Fragment(), View.OnClickListener {

    private var mPreviousTimeZone: TimeZone? = null
    private var mTimeZone: TimeZone? = null
    private var mTimeZoneInfo: TimeZoneInfo? = null
    private val mDate = Date()
    private var mAction: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_zone, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as TimeZonePickerActivity).apply {
            supportActionBar?.title = getString(R.string.timezone_fragment_title)
            mPreviousTimeZone = TimeZone.getTimeZone(mTimeZoneId)
        }

        arguments?.let {
            val id = it.getString(TimeZonePickerActivity.TIME_ZONE_ID, TimeZone.getDefault().id)

            with(TimeZone.getTimeZone(id)) {
                mTimeZone = this
                mTimeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), mDate).format(this)
            }

            mAction = it.getInt(TimeZonePickerActivity.ACTION)
        }

        if(mPreviousTimeZone != mTimeZone || mAction == TimeZonePickerActivity.ACTION_ADD) {
            time_zone_apply.visibility = View.VISIBLE
            if(mAction == TimeZonePickerActivity.ACTION_ADD) {
                time_zone_apply.text = "Add"
            }
        }

        updateSummariesByTimeZone()

        time_zone_country_layout.setOnClickListener(this)
        time_zone_region_layout.setOnClickListener(this)
        time_zone_apply.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val bundle = Bundle().apply {
            putString(TimeZonePickerActivity.ORIGINAL_TIME_ZONE_ID, mPreviousTimeZone?.id)
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
            R.id.time_zone_apply -> {
                if(mAction == TimeZonePickerActivity.ACTION_ADD) {
                    val clockList = DatabaseCursor(context!!).getClockList()
                    var isExist = false
                    clockList.forEach {
                        if(it.timezone == mTimeZone?.id) {
                            isExist = true
                        }
                    }
                    if(isExist) {
                        Toast.makeText(context, resources.getString(R.string.exist_timezone), Toast.LENGTH_SHORT).show()
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
        time_zone_country_summary.text = MediaCursor.getCountryNameByTimeZone(mTimeZone)
        with(mTimeZoneInfo) {
            if(this == null) {
                time_zone_region_layout.isEnabled = false
                time_zone_region_summary.text = getString(R.string.time_zone_unknown)
            }
            else {
                time_zone_region_layout.isEnabled = true
                var regionName = this.mExemplarName
                if(regionName == null) {
                    regionName =
                            if(this.mTimeZone.inDaylightTime(mDate)) this.mDaylightName
                            else this.mStandardName
                }
                if(regionName == null) regionName = this.mTimeZone.id
                time_zone_region_summary.text = getString(R.string.timezone_format, regionName, this.mGmtOffset)

                val found = MediaCursor.getULocaleByTimeZoneId(mTimeZone.id)
                val isSingleTimeZone = found != null && (MediaCursor.getTimeZoneListByCountry(found.country).size == 1)
                time_zone_region_layout.apply {
                    isEnabled = !isSingleTimeZone
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimeZoneFragment()
    }

}
