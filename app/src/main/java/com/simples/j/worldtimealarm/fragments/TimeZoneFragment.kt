package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.etc.TimeZoneInfo
import com.simples.j.worldtimealarm.utils.DatabaseCursor
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_time_zone.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
class TimeZoneFragment : Fragment(), View.OnClickListener, Toolbar.OnMenuItemClickListener {

    private lateinit var fragmentContext: Context

    private var mPreviousTimeZone: TimeZone? = null
    private var mTimeZone: TimeZone? = null
    private var mTimeZoneInfo: TimeZoneInfo? = null
    private val mDate = Date()
    private var mAction: Int = -1
    private var mType: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_zone, container, false)
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

            if(mType == TimeZonePickerActivity.TYPE_ALARM_CLOCK) {
                time_zone_change_info.visibility = View.VISIBLE
                time_zone_change_info.text = getString(R.string.time_zone_change_warning)
            }

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
                    if(item != null) {
                        Snackbar.make(fragment_layout, getString(R.string.exist_timezone), Snackbar.LENGTH_LONG)
                                .setAnchorView(bottomAppBar)
                                .show()
                    }

                    bottomAppBar.menu?.findItem(R.id.action_apply)?.isEnabled = item == null
                }
                TimeZonePickerActivity.ACTION_CHANGE -> {
                    time_zone_change_info.visibility = View.VISIBLE

                    val locale = Locale.getDefault()
                    val previous = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mPreviousTimeZone), MediaCursor.getGmtOffsetString(locale, mPreviousTimeZone, mDate))
                    val current = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), MediaCursor.getGmtOffsetString(locale, mTimeZone, mDate))
                    time_zone_change_info.text = getString(R.string.time_zone_change_info, previous, current)
                }
            }
        }

        updateSummariesByTimeZone()

        time_zone_country_layout.setOnClickListener(this)
        time_zone_region_layout.setOnClickListener(this)
        bottomAppBar.setOnMenuItemClickListener(this)

        if(mAction == TimeZonePickerActivity.ACTION_ADD) {
            bottomAppBar.menu?.findItem(R.id.action_apply)?.title = getString(R.string.time_zone_add)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.action_apply -> {
                if(mAction == TimeZonePickerActivity.ACTION_ADD) {
                    if(mTimeZone == null) {
                        Snackbar.make(fragment_layout, getString(R.string.time_zone_select), Snackbar.LENGTH_SHORT)
                                .setAnchorView(bottomAppBar)
                                .show()
                        return false
                    }
                }

                activity?.run {
                    val intent = Intent()
                    intent.putExtra(TimeZoneSearchActivity.TIME_ZONE_ID, mTimeZone?.id)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                return true
            }
            else -> false
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
        }
    }

    private fun updateSummariesByTimeZone() {
        with(MediaCursor.getCountryNameByTimeZone(mTimeZone)) {
            if(this.isNotEmpty()) time_zone_country_summary.text = this
        }

        with(mTimeZoneInfo) {
            if(this == null) {
                time_zone_region_layout.isEnabled = false
            }
            else {
                time_zone_region_layout.isEnabled = true
                time_zone_region_summary.text = getString(R.string.timezone_format, MediaCursor.getBestNameForTimeZone(mTimeZone), this.mGmtOffset)

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
