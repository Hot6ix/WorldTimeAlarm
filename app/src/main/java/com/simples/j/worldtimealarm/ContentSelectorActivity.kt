package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.databinding.ActivityContentSelectorBinding
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import com.simples.j.worldtimealarm.fragments.ContentSelectorFragment
import com.simples.j.worldtimealarm.fragments.DatePickerFragment
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel
import java.util.*

class ContentSelectorActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var viewModel: ContentSelectorViewModel
    private lateinit var contentSelectorFragment: ContentSelectorFragment
    private lateinit var datePickerFragment: DatePickerFragment
    private lateinit var binding: ActivityContentSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContentSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

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

        viewModel = ViewModelProvider(this).get(ContentSelectorViewModel::class.java)

        if(savedInstanceState == null) {
            viewModel.action = intent.action

            viewModel.lastSelectedValue = intent.getSerializableExtra(LAST_SELECTED_KEY)
            intent.getLongExtra(START_DATE_KEY, -1).let {
                viewModel.startDate.value =
                        if(it > 0) it
                        else null
            }
            intent.getLongExtra(END_DATE_KEY, -1).let {
                viewModel.endDate.value =
                        if(it > 0) it
                        else null
            }
            viewModel.timeZone = intent.getStringExtra(TIME_ZONE_KEY) ?: TimeZone.getDefault().id

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
        when(intent.action) {
            ACTION_REQUEST_AUDIO, ACTION_REQUEST_VIBRATION, ACTION_REQUEST_SNOOZE -> {
                setResult(Activity.RESULT_OK, getResult())
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.action_apply -> {
                setResult(Activity.RESULT_OK, getResult())
                finish()

                true
            }
            else -> false
        }
    }

    private fun getResult(): Intent {
        return Intent().apply {
            when(intent.action) {
                ACTION_REQUEST_AUDIO -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as RingtoneItem?)
                }
                ACTION_REQUEST_VIBRATION -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as PatternItem?)
                }
                ACTION_REQUEST_SNOOZE -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as SnoozeItem?)
                }
                ACTION_REQUEST_DATE -> { }
            }
        }
    }

    companion object {
        const val LAST_SELECTED_KEY = "LAST_SELECTED_KEY"
        const val START_DATE_KEY = "START_DATE_KEY"
        const val END_DATE_KEY = "END_DATE_KEY"
        const val TIME_ZONE_KEY = "TIME_ZONE_KEY"

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
