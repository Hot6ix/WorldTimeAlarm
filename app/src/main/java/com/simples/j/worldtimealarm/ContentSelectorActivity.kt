package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import com.simples.j.worldtimealarm.fragments.ContentSelectorFragment
import com.simples.j.worldtimealarm.fragments.DatePickerFragment
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel

class ContentSelectorActivity : AppCompatActivity() {

    private lateinit var viewModel: ContentSelectorViewModel
    private lateinit var contentSelectorFragment: ContentSelectorFragment
    private lateinit var datePickerFragment: DatePickerFragment

    private var lastValue: Any? = null
    private var startDate: Long = -1
    private var endDate: Long? = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_selector_activity)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        lastValue = intent.getSerializableExtra(LAST_SELECTED_KEY)
        startDate = intent.getLongExtra(START_DATE_KEY, -1)
        endDate = intent.getLongExtra(END_DATE_KEY, -1)

        supportActionBar?.title = when(intent.action) {
            ACTION_REQUEST_AUDIO -> {
                getString(R.string.select_ringtone)
            }
            ACTION_REQUEST_VIBRATION -> {
                getString(R.string.select_vibration)
            }
            ACTION_REQUEST_SNOOZE -> {
                getString(R.string.select_snooze)
            }
            ACTION_REQUEST_DATE -> {
                getString(R.string.date)
            }
            else -> {
                getString(R.string.app_name)
            }
        }

        viewModel = ViewModelProvider(this).get(ContentSelectorViewModel::class.java).also {
            it.action = intent.action
            if(it.lastSelectedValue == null) it.lastSelectedValue = lastValue
            if(it.startDate.value == null) it.startDate.value = startDate
            if(it.endDate.value == null) it.endDate.value = endDate
            if(it.defaultStart == null) it.defaultStart = startDate
            if(it.defaultEnd == null) it.defaultEnd = endDate
        }

        if(savedInstanceState == null) {
            when (intent.action) {
                ACTION_REQUEST_AUDIO, ACTION_REQUEST_VIBRATION, ACTION_REQUEST_SNOOZE -> {
                    with(supportFragmentManager.findFragmentByTag(ContentSelectorFragment.TAG)) {
                        if (this != null) {
                            contentSelectorFragment = this as ContentSelectorFragment
                        } else {
                            contentSelectorFragment = ContentSelectorFragment.newInstance()
                            supportFragmentManager
                                    .beginTransaction()
                                    .replace(R.id.container, contentSelectorFragment, ContentSelectorFragment.TAG)
                                    .commitNow()
                        }
                    }
                }
                ACTION_REQUEST_DATE -> {
                    with(supportFragmentManager.findFragmentByTag(DatePickerFragment.TAG)) {
                        if (this != null) {
                            datePickerFragment = this as DatePickerFragment
                        } else {
                            datePickerFragment = DatePickerFragment.newInstance()
                            supportFragmentManager
                                    .beginTransaction()
                                    .replace(R.id.container, datePickerFragment, DatePickerFragment.TAG)
                                    .commitNow()
                        }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    override fun onBackPressed() {
//        if(intent.action == ACTION_REQUEST_DATE) {
//            val s = viewModel.startDate.value
//            val e = viewModel.endDate.value
//
//            if(s != null && e != null) {
//                if(s > 0 && e > 0) {
//                    if(s > e || s == e) {
//                        return
//                    }
//                }
//            }
//            if(e != null && DateUtils.isToday(e)) return
//        }
        setResult(Activity.RESULT_OK, getResult())
        finish()
    }

    private fun getResult(): Intent {
        return Intent().apply {
            when(intent.action) {
                ACTION_REQUEST_AUDIO -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as RingtoneItem)
                }
                ACTION_REQUEST_VIBRATION -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as PatternItem)
                }
                ACTION_REQUEST_SNOOZE -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as SnoozeItem)
                }
                ACTION_REQUEST_DATE -> {
                    putExtra(START_DATE_KEY, viewModel.startDate.value)
                    putExtra(END_DATE_KEY, viewModel.endDate.value)
                }
            }
        }
    }

    companion object {
        const val LAST_SELECTED_KEY = "LAST_SELECTED_KEY"
        const val START_DATE_KEY = "START_DATE_KEY"
        const val END_DATE_KEY = "END_DATE_KEY"

        const val AUDIO_REQUEST_CODE = 2
        const val VIBRATION_REQUEST_CODE = 3
        const val SNOOZE_REQUEST_CODE = 4
        const val USER_AUDIO_REQUEST_CODE = 5
        const val DATE_REQUEST_CODE = 6
        const val ACTION_REQUEST_AUDIO = "ACTION_REQUEST_AUDIO"
        const val ACTION_REQUEST_VIBRATION = "ACTION_REQUEST_VIBRATION"
        const val ACTION_REQUEST_SNOOZE = "ACTION_REQUEST_SNOOZE"
        const val ACTION_REQUEST_DATE = "ACTION_REQUEST_DATE"
    }
}
