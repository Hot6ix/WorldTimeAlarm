package com.simples.j.worldtimealarm

import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.simples.j.worldtimealarm.fragments.TimeZoneFragment
import com.simples.j.worldtimealarm.fragments.TimeZonePickerFragment

@RequiresApi(Build.VERSION_CODES.N)
class TimeZonePickerActivity : AppCompatActivity(), TimeZonePickerFragment.OnTimeZoneChangeListener {

    private var mTimeZoneId: String? = null
    private lateinit var mTimeZoneFragment: TimeZoneFragment
    private lateinit var mTimeZonePickerFragment: TimeZonePickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_zone_picker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.timezone_fragment_title)

        mTimeZoneId = intent.getStringExtra(TIME_ZONE_ID)

        if(supportFragmentManager.findFragmentByTag(TIME_ZONE_FRAGMENT_TAG) == null) {
            mTimeZoneFragment = TimeZoneFragment.newInstance().apply {
                val bundle = Bundle()
                bundle.putString(TIME_ZONE_ID, mTimeZoneId)
                arguments = bundle
            }
            supportFragmentManager.beginTransaction().add(R.id.time_zone_picker_fragment_container, mTimeZoneFragment, TIME_ZONE_FRAGMENT_TAG).commit()
        }
        else {
            mTimeZoneFragment = supportFragmentManager.findFragmentByTag(TIME_ZONE_FRAGMENT_TAG) as TimeZoneFragment
        }
        mTimeZonePickerFragment = supportFragmentManager.findFragmentByTag(TIME_ZONE_PICKER_FRAGMENT_TAG) as? TimeZonePickerFragment ?: TimeZonePickerFragment.newInstance()
        mTimeZonePickerFragment.setListener(this)
    }

    fun startPickerFragment() {
        supportFragmentManager.beginTransaction()
                .add(R.id.time_zone_picker_fragment_container, mTimeZonePickerFragment, TIME_ZONE_PICKER_FRAGMENT_TAG)
                .hide(mTimeZoneFragment)
                .addToBackStack(TIME_ZONE_PICKER_FRAGMENT_TAG)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCountryChanged(country: String) {
    }

    override fun onTimeZoneChanged(timeZone: String) {
    }

    companion object {
        const val TIME_ZONE_ID = "TIME_ZONE_ID"
        const val TIME_ZONE_FRAGMENT_TAG = "TIME_ZONE_FRAGMENT_TAG"
        const val TIME_ZONE_PICKER_FRAGMENT_TAG = "TIME_ZONE_PICKER_FRAGMENT_TAG"
    }
}
