package com.simples.j.worldtimealarm.fragments


import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneFragment : Fragment(), View.OnClickListener {

    private var mTimeZone: TimeZone? = null
    private var mTimeZoneInfo: TimeZoneInfo? = null
    private val mDate = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_zone, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments.let {
            if(mTimeZone == null) {
                val id = it?.getString(TimeZonePickerActivity.TIME_ZONE_ID, TimeZone.getDefault().id)
                with(TimeZone.getTimeZone(id)) {
                    mTimeZone = this
                    mTimeZoneInfo = TimeZoneInfo.Formatter(Locale.getDefault(), mDate).format(this)
                }
            }
        }

        setSummariesByTimeZone()

        time_zone_country_layout.setOnClickListener(this)
        time_zone_region_layout.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.time_zone_country_layout -> {
                (activity as? TimeZonePickerActivity).run {
                    this?.startPickerFragment()
                }
            }
        }
    }

    fun setSummariesByTimeZone() {
        time_zone_country_summary.text = MediaCursor.getCountryNameByTimeZone(mTimeZone)
        with(mTimeZoneInfo) {
            time_zone_region_summary.text = if(this == null) {
                getString(R.string.unknown_timezone)
            }
            else {
                var regionName = this.mExemplarName
                if(regionName == null) {
                    regionName =
                            if(this.mTimeZone.inDaylightTime(mDate)) this.mDaylightName
                            else this.mStandardName
                }
                if(regionName == null) this.mGmtOffset
                else getString(R.string.timezone_format, regionName, this.mGmtOffset)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimeZoneFragment()
    }

}
