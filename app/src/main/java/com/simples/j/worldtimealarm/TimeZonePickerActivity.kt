package com.simples.j.worldtimealarm

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.simples.j.worldtimealarm.fragments.TimeZoneFragment
import com.simples.j.worldtimealarm.fragments.TimeZonePickerFragment

@RequiresApi(Build.VERSION_CODES.N)
class TimeZonePickerActivity : AppCompatActivity(), TimeZonePickerFragment.OnTimeZoneChangeListener {

    var mTimeZoneId: String? = null
    private var mAction: Int = -1
    private var mType: Int = -1
    private lateinit var mTimeZoneFragment: TimeZoneFragment
    private lateinit var mTimeZonePickerFragment: TimeZonePickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_zone_picker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mTimeZoneId = intent.getStringExtra(TIME_ZONE_ID)
        mAction = intent.getIntExtra(ACTION, 0)
        mType = intent.getIntExtra(TYPE, -1)

        if(supportFragmentManager.findFragmentByTag(TIME_ZONE_FRAGMENT_TAG) == null) {
            mTimeZoneFragment = TimeZoneFragment.newInstance().apply {
                val bundle = Bundle().apply {
                    putString(TIME_ZONE_ID, mTimeZoneId)
                    putInt(ACTION, mAction)
                    putInt(TYPE, mType)
                }
                arguments = bundle
            }
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.time_zone_picker_fragment_container, mTimeZoneFragment, TIME_ZONE_FRAGMENT_TAG)
                    .commit()
        }
        else {
            mTimeZoneFragment = supportFragmentManager.findFragmentByTag(TIME_ZONE_FRAGMENT_TAG) as TimeZoneFragment
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                if(supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
                else finish()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun startPickerFragment(bundle: Bundle? = null, tag: String) {
        mTimeZonePickerFragment = supportFragmentManager.findFragmentByTag(tag) as? TimeZonePickerFragment ?: TimeZonePickerFragment.newInstance(this)
        mTimeZonePickerFragment.arguments = bundle
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit, R.anim.fragment_close_enter, R.anim.fragment_close_exit)
                .replace(R.id.time_zone_picker_fragment_container, mTimeZonePickerFragment, tag)
                .addToBackStack(tag)
                .commit()
    }

    override fun onTimeZoneChanged(timeZone: String) {
        val bundle = Bundle().apply {
            putString(TIME_ZONE_ID, timeZone)
            putInt(ACTION, mAction)
            putInt(TYPE, mType)
        }
        mTimeZoneFragment.arguments = bundle
    }

    companion object {
        const val TIME_ZONE_ID = "TIME_ZONE_ID"
        const val TIME_ZONE_FRAGMENT_TAG = "TIME_ZONE_FRAGMENT_TAG"
        const val TIME_ZONE_PICKER_FRAGMENT_COUNTRY_TAG = "TIME_ZONE_PICKER_FRAGMENT_COUNTRY_TAG"
        const val TIME_ZONE_PICKER_FRAGMENT_TIME_ZONE_TAG = "TIME_ZONE_PICKER_FRAGMENT_TIME_ZONE_TAG"

        const val REQUEST_TYPE = "REQUEST_TYPE"
        const val GIVEN_COUNTRY = "GIVEN_COUNTRY"
        const val REQUEST_COUNTRY = 0
        const val REQUEST_TIME_ZONE = 1

        const val ACTION = "ACTION"
        const val ACTION_ADD = 1
        const val ACTION_CHANGE = 2

        const val TYPE = "TYPE"
        const val TYPE_ALARM_CLOCK = 0
        const val TYPE_WORLD_CLOCK = 1
    }
}
